package ru.kv.client;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.RandomStringUtils;
import ru.kv.hash.JumpConsistentHash;
import ru.kv.util.Endpoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

public class ClientCli {
    private static final long SLEEP_TIME = 1000;
    private static final int LIMIT_SERVERS_COUNT = 100;

    private static HashMap<Integer, Endpoint> endpointHashMap = new HashMap();
    private static HashMap<Integer,RPCClientConnectionProxy> rpcClientConnectionProxyHashMap = new HashMap();
    private static int bucketCount=0;

    public static void main (String[] args) throws Exception {

        // ex "-s localhost:3990,localhost:3991,localhost:3999"
        String[] hosts = getCliHelper(args);

        bucketCount = hosts.length;

        for (int i=0;i<hosts.length;i++) {
            String [] str=hosts[i].split(":");
            endpointHashMap.put(i,new Endpoint(str[0],Integer.valueOf( str[1])));
        }

        for (int i=0;i<hosts.length;i++) {
           // System.out.println("i:"+i+",hosts:"+hosts[i]);
            RPCClient rpcClient= new RPCClient();
            rpcClient.start();
            RPCClientConnectionProxy connectionProxy = new RPCClientConnectionProxy(rpcClient,endpointHashMap.get(i));
            rpcClientConnectionProxyHashMap.put(i,connectionProxy);
        }

        for (int i=0;i<rpcClientConnectionProxyHashMap.size() ;i++) {
            //System.out.println("i_0:"+i+",hosts:"+hosts);
            try {
                if (!rpcClientConnectionProxyHashMap.get(i).isConnectionEstablished()) {
                    System.out.println("Task: " + Thread.currentThread().getName() + " Waiting for active connection");
                    Thread.sleep(SLEEP_TIME);
                }
            } catch (InterruptedException ie) {
                System.out.println("Cli thread got interrupted");
                ie.printStackTrace();
            }
        }

        for (;;) {
            System.out.print(">");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line = in.readLine();
            if (line == null) {

                System.out.println("Inccorrect command! ex: get 1");
                System.out.println("ex: put 1:one");
                break;
            }
            //System.out.println("LINE:" + line + "LINE");

            if (line.toLowerCase().startsWith("get")) {
                String keyStr = line.split(" ")[1];
                //System.out.println("key:"+keyStr);
                long key = Long.valueOf( keyStr);
                int jumpConsistentHash = JumpConsistentHash.jumpConsistentHash(key,bucketCount);
               // System.out.println("jumpConsistentHash:"+jumpConsistentHash);
                rpcClientConnectionProxyHashMap.get(jumpConsistentHash).send(new KVClientRequest(key, null));
                System.out.print(">");

            } else if (line.toLowerCase().startsWith("put")) {
                // TODO FIX
                String keyV = line.split(" ")[1];
               // System.out.println("keyV:"+keyV);
                String keyStr = keyV.split(":")[0];
                String value = keyV.split(":")[1];
                // System.out.println("key:"+keyStr+",v:"+value);

                try {
                    Long key = Long.valueOf(keyStr);
                    int jumpConsistentHash = JumpConsistentHash.jumpConsistentHash(key,bucketCount);
                   // System.out.println("jumpConsistentHash:"+jumpConsistentHash);
                    rpcClientConnectionProxyHashMap.get(jumpConsistentHash).send(new KVClientRequest(key, value));
                } catch  (Exception e) {
                    System.out.println(e.getMessage());
                    System.exit(1);
                }


            } else if (line.toLowerCase().startsWith("quit")) {

                for (int i=0; i<rpcClientConnectionProxyHashMap.size();i++) {
                    if (rpcClientConnectionProxyHashMap.get(i).isConnectionEstablished()) {
                        try {
                            rpcClientConnectionProxyHashMap.get(i).close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                break;
            } else {
                System.out.println("Inccorrect command! ex get 1  ,ex put 1:one");
            }
        }

        System.exit(0);
    }

    private static String[] getCliHelper(String[] args) {
        String[] hostArray = new String[LIMIT_SERVERS_COUNT];
        Options options = new Options();
        Option config = Option.builder("s").longOpt("servers")
                .argName("servers")
                .hasArg()
                .required(true)
                .desc("set servers list, ex -s localhost:9999,localhost:8888").build();
        options.addOption(config);

        CommandLine cmd;
        CommandLineParser parser = new BasicParser();
        HelpFormatter helper = new HelpFormatter();

        try {
            cmd = parser.parse(options, args);
            if(cmd.hasOption("s")) {
                String opt_config = cmd.getOptionValue("servers");
                System.out.println("servers: " + opt_config);
                hostArray = opt_config.split(",");
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(0);
        }
        
        return hostArray;
    }

}
