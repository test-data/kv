package ru.kv.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import ru.kv.protobuf.KVRPC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The server side of RPC layer on an endpoint
 */
public class RPCServer implements AutoCloseable {

    private final EventLoopGroup connectionAcceptorGroup;
    private final EventLoopGroup acceptedConnectionGroup;
    private final Executor asyncWorkerPool;
    private final int port;
    private final KeyValueMap store;
    private Channel serverChannel;

    public RPCServer(final int port, final KeyValueMap store, final int numAsyncPoolThreads) {
        /*
         * Two event loop groups are used:
         * (1) First group is responsible for accepting connections.
         *     For each such accepted connection, it creates a
         *     SocketChannel and assigns that channel to a particular
         *     event loop (single threaded) in another event loop group.
         * (2) This event loop in the second event loop group
         *     will be responsible for handling all the activity
         *     on this accepted connection (channel). A channel will be
         *     assigned to exactly one event loop but multiple channels
         *     can be handled by a single event loop
         */
        this.connectionAcceptorGroup = new NioEventLoopGroup();
        this.acceptedConnectionGroup = new NioEventLoopGroup();
        this.asyncWorkerPool = Executors.newFixedThreadPool(numAsyncPoolThreads);
        this.port = port;
        this.store = store;
    }

    public void start() throws Exception {
        final ChannelInitializer<SocketChannel> channelInitializer =
                new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // the bytes sent by the client over the connection to server
                        // will be decoded into protobuf with appropriate handlers
                        // in the channel pipeline
                        final ChannelPipeline pipeline = ch.pipeline();
                        // inbound handler to split the received ByteBuf and get the protobuf data
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
                        // inbound handler to decode data from ByteBuf into RPCRequest protobuf
                        pipeline.addLast(new ProtobufDecoder(KVRPC.RPCRequest.getDefaultInstance()));
                        // outbound handler to prepend the length field to protobuf response from server
                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                        // outbound handler to encode the protobuf response from server into bytes for client
                        pipeline.addLast(new ProtobufEncoder());
                        // inbound handler to parse the data in RPCRequest protobuf, handle the
                        // request, create and send RPCResponse
                        pipeline.addLast(new RPCRequestHandler());
                    }
                };

        final ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(connectionAcceptorGroup, acceptedConnectionGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(channelInitializer);

        try {
            serverChannel = serverBootstrap.bind(port).sync().channel();
            System.out.println("RPC services are up. Server listening for requests on port: " + port);
        } catch (Exception e) {
            System.out.println("Failed to bind RPC server to port " + port);
            throw e;
        }
    }

    /**
     * Inbound handler for handling the requests/data arriving
     * from RPC client endpoints in the cluster. The same instance of
     * channel handler is installed in the pipeline
     * of all {@link SocketChannel} opened for connections. Since
     * the handler internally works with a thread safe store API,
     * the handler is sharable.
     */
    @ChannelHandler.Sharable
    class RPCRequestHandler extends SimpleChannelInboundHandler<KVRPC.RPCRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, KVRPC.RPCRequest rpcRequest) {
            handleRequest(ctx, rpcRequest);
        }
    }

    private void handleRequest(
            final ChannelHandlerContext ctx,
            final KVRPC.RPCRequest rpcRequest) {
        final CompletableFuture<KVRPC.RPCResponse> responseFuture;
        if (rpcRequest.hasGetRequest()) {
            System.out.println("Server received GET request. Request seq num: " + rpcRequest.getSequenceNum());
            responseFuture = CompletableFuture.supplyAsync(() -> {
                // async GET computation to be run in the worker pool thread
                final KVRPC.GetRequest getRequest = rpcRequest.getGetRequest();
                final long sequenceNum = rpcRequest.getSequenceNum();
                //rpcRequest.
                final KeyValueMap.OpResult result = store.get(getRequest.getKey());
                final KVRPC.GetResponse response =
                        KVRPC.GetResponse.newBuilder()
                                .setFound(result.returnCode == KeyValueMap.ReturnCode.SUCCESS)
                                .setKey(getRequest.getKey())
                                .setValue(result.data)
                                .build();
                return KVRPC.RPCResponse
                        .newBuilder()
                        .setGetResponse(response)
                        .setSequenceNum(sequenceNum)
                        .build();
            }, asyncWorkerPool);
        } else {
            if (rpcRequest.getSequenceNum()%10000==0) {
                System.out.println("Server received PUT request. Request seq num: " + rpcRequest.getSequenceNum());
            }
            responseFuture = CompletableFuture.supplyAsync(() -> {
                // async PUT computation to be run in the worker pool thread
                final KVRPC.PutRequest putRequest = rpcRequest.getPutRequest();
                final long sequenceNum = rpcRequest.getSequenceNum();
                final KeyValueMap.OpResult result = store.put(putRequest.getKey(), putRequest.getValue());
                final KVRPC.PutResponse response =
                        KVRPC.PutResponse.newBuilder()
                                .setSuccess(result.returnCode == KeyValueMap.ReturnCode.SUCCESS)
                                .setKey(putRequest.getKey())
                                .setValue(putRequest.getValue())
                                .build();
                return KVRPC.RPCResponse
                        .newBuilder()
                        .setPutResponse(response)
                        .setSequenceNum(sequenceNum)
                        .build();
            }, asyncWorkerPool);
        }
        // callback code sends the RPCResponse back to client on
        // the completion of future
        responseFuture.thenAccept(response -> ctx.writeAndFlush(response));
    }

    @Override
    public void close() throws Exception {
        try {
            System.out.println("Shutting down RPC server");
            serverChannel.close().sync();
        } catch (Exception e) {
            System.out.println("Failure while shutting down RPC server");
            throw e;
        } finally {
            try {
                connectionAcceptorGroup.shutdownGracefully().sync();
                acceptedConnectionGroup.shutdownGracefully().sync();
            } catch (InterruptedException ie) {
                System.out.println("RPC server interrupted while shutting down event loop groups");
            }
        }
    }
}

