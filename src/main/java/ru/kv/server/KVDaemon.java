package ru.kv.server;

import ru.kv.client.RPCClient;
import org.apache.commons.cli.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * The main KV daemon process that is started on each
 * node in the cluster
 */
public class KVDaemon {

    private static RPCClient rpcClient;
    private static RPCServer rpcServer;

    public static void main (String[] args) throws Exception {


        //parse port & thread
        List<Integer> portThread = new ArrayList<>();
        portThread = getCliHelper(args);

        final int serverRPCPort = portThread.get(0);
        final int serverAsyncPoolThreads = portThread.get(1);

        try {
            // start RPC client for this endpoint
            rpcClient = new RPCClient();
            rpcClient.start();

            // start RPC server for this endpoint
            KeyValueMap store = new KeyValueMap();
            rpcServer = new RPCServer(serverRPCPort, store, serverAsyncPoolThreads);
            rpcServer.start();
        } catch (Exception e) {
            System.out.println("Failure while starting KVStore.");
            // revisit this for proper cleanup
            throw e;
        }

        final String host = InetAddress.getLocalHost().getHostAddress();
        System.out.println("KVStore daemon started on node: " + host);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    rpcServer.close();
                    rpcClient.close();
                } catch (Exception e) {
                    System.out.println("Failure while shutting down KVStore");
                    e.printStackTrace();
                }
            }
        });
    }

    public static List<Integer> getCliHelper(String[] args){
        List<Integer> portThread = new ArrayList<>();
        Options options = new Options();
        Option config = Option.builder("p").longOpt("port")
                .argName("port")
                .hasArg()
                .required(true)
                .desc("set port").build();
        options.addOption(config);
        Option thread = Option.builder("t").longOpt("thread")
                .argName("thread")
                .hasArg()
                .required(true)
                .desc("set thread count").build();
        options.addOption(thread);
        // define parser
        CommandLine cmd;
        CommandLineParser parser = new BasicParser();
        HelpFormatter helper = new HelpFormatter();

        try {
            cmd = parser.parse(options, args);
            if(cmd.hasOption("p")) {
                String opt_config = cmd.getOptionValue("port");
                System.out.println("port: " + opt_config);
                portThread.add(Integer.valueOf(opt_config));
            }

            if (cmd.hasOption("t")) {
                String opt_config = cmd.getOptionValue("thread");
                System.out.println("thread: " + opt_config);
                portThread.add(Integer.valueOf(opt_config));
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(0);
        }
        return portThread;
    }
}
