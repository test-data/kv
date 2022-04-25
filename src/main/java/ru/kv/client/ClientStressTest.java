package ru.kv.client;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.RandomStringUtils;
import ru.kv.hash.JumpConsistentHash;
import ru.kv.util.Endpoint;
import java.util.HashMap;

public class ClientStressTest  {
    private static final long SLEEP_TIME = 1000;
    private static final long LIMIT_RANDOM_KEY = 1_000_000_000;
    private static final int LIMIT_SERVERS_COUNT = 100;

    private static HashMap<Integer, Endpoint> endpointHashMap = new HashMap();
    private static HashMap<Integer,RPCClientConnectionProxy> rpcClientConnectionProxyHashMap = new HashMap();
    private static int bucketCount=0;
    private static Long countMsg=0l;

    public static void main (String[] args) throws Exception {

        String[] hosts = getCliHelper(args);

        bucketCount = hosts.length;

        for (int i=0;i<hosts.length;i++) {
            String [] str=hosts[i].split(":");
            endpointHashMap.put(i,new Endpoint(str[0],Integer.valueOf( str[1])));
        }

        for (int i=0;i<hosts.length;i++) {
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
        Long cnt=0l;

        // generated and send key/value pair
        while (cnt < countMsg) {
            try {
                final Long key = 1l + (long) (Math.random() * (LIMIT_RANDOM_KEY - 1l));
                final String value = RandomStringUtils.randomAlphabetic(10);
                int jumpConsistentHash = JumpConsistentHash.jumpConsistentHash(key,bucketCount);
                rpcClientConnectionProxyHashMap.get(jumpConsistentHash).send(new KVClientRequest(key, value));
                ++cnt;
            } catch  (Exception e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }

        }

    }

    private static String[] getCliHelper(String[] args) {
        String[] hostArray = new String[LIMIT_SERVERS_COUNT];
        Options options = new Options();
        Option servers = Option.builder("s").longOpt("servers")
                .argName("servers")
                .hasArg()
                .required(true)
                .desc("set servers list, ex -s localhost:9999,localhost:8888").build();
        options.addOption(servers);

        Option config = Option.builder("c").longOpt("count")
                .argName("count")
                .hasArg()
                .required(true)
                .desc("set the number of generated and sent to KVServer key/value pairs").build();
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
            if(cmd.hasOption("c")) {
                String opt_config = cmd.getOptionValue("count");
                System.out.println("count: " + opt_config);
                countMsg = Long.valueOf(opt_config);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(0);
        }

        return hostArray;
    }
}

