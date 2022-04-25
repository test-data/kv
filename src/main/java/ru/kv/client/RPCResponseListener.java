package ru.kv.client;


import ru.kv.protobuf.KVRPC;

class RPCResponseListener {

    void done(KVRPC.RPCResponse response) {
        if (response.hasGetResponse()) {
            KVRPC.GetResponse getResponse = response.getGetResponse();
            //System.out.println("RPC Response received for GET request: " + response.getSequenceNum());
            if (getResponse.getFound()) {
                System.out.println("Key: " + getResponse.getKey() + " Value: " + getResponse.getValue());
               // System.out.print(">");
            } else {
                System.out.println("Key: " + getResponse.getKey() + " Value: DOES NOT EXIST");
               // System.out.print(">");
            }
        } else {
            KVRPC.PutResponse putResponse = response.getPutResponse();
            //System.out.println("RPC Response for PUT request: " + response.getSequenceNum());
            if (putResponse.getSuccess()) {
                //System.out.println("Key: " + putResponse.getKey() + " Value: " + putResponse.getValue() + " stored in server");
            } else {
                System.out.println("Key: " + putResponse.getKey() + " Value: " + putResponse.getValue() + " failed to store in server");
            }
        }
    }
}
