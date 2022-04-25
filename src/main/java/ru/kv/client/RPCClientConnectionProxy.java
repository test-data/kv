package ru.kv.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import ru.kv.protobuf.KVRPC;
import ru.kv.util.Endpoint;

/**
 * Creates and holds the RPC connection to an endpoint
 * (remote or local).
 */
public class RPCClientConnectionProxy implements AutoCloseable {

    private final RPCClient rpcClient;
    private final Endpoint endpoint;
    private Channel channel;

    public RPCClientConnectionProxy (final RPCClient rpcClient, final Endpoint endpoint) {
        this.rpcClient = rpcClient;
        this.endpoint = endpoint;
        establishConnection();
    }

    /**
     * Connect to an endpoint
     */
    private void establishConnection() {
        final ChannelFuture future = rpcClient.connectToPeer(endpoint);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                channel = future.channel();
                if (future.isSuccess()) {
                    System.out.println("Connected to peer: " + channel.remoteAddress());
                } else {
                    System.out.println("Unable to establish connection with peer: " + endpoint.getAddress());
                    channel.close().sync();
                }
            }
        });
    }

    // Checks if the connection has been established or not
    public boolean isConnectionEstablished() {
        return channel != null && channel.isActive();
    }

    public void send(final KVClientRequest request) {
        final long sequenceNum = rpcClient.getNextSequenceNum();
        KVRPC.RPCRequest rpcRequest;
        if (request.getValue() == null) {
            KVRPC.GetRequest getRequest = KVRPC.GetRequest
                    .newBuilder()
                    .setKey(request.getKey())
                    .build();
            rpcRequest = KVRPC.RPCRequest
                    .newBuilder()
                    .setGetRequest(getRequest)
                    .setSequenceNum(sequenceNum)
                    .build();
        } else {
            KVRPC.PutRequest putRequest = KVRPC.PutRequest
                    .newBuilder()
                    .setKey(request.getKey())
                    .setValue(request.getValue())
                    .build();
            rpcRequest = KVRPC.RPCRequest
                    .newBuilder()
                    .setPutRequest(putRequest)
                    .setSequenceNum(sequenceNum)
                    .build();
        }

        final ChannelFuture sendFuture = channel.writeAndFlush(rpcRequest);
        sendFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    //  System.out.println("Request# " + sequenceNum + " sent successfully to " + future.channel().remoteAddress());
                } else {
                    System.out.println("Unable to send request# " + sequenceNum);
                }
            }
        });
    }

    @Override
    public void close() throws Exception {
        try {
            System.out.println("ConnectionProxy: Closing channel to peer " + channel.remoteAddress());
            //channel.closeFuture().sync();
            channel.close();
        } catch (Exception e) {
            System.out.println("Failure while shutting down connection proxy");
            throw e;
        }
    }
}

