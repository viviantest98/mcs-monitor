package com.mapr.qa;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.mapr.db.MapRDB;
import com.mapr.db.Table;
import org.ojai.Document;
import org.ojai.store.Connection;
import org.ojai.store.DocumentStore;
import org.ojai.store.DriverManager;

import java.io.IOException;

import static java.lang.System.exit;


public class Monitor {
    static String TABLE_PATH = "/user/mapr/mcs/uptime";
    //static int POLLING_INTERVAL = 60000; //1 min in miliseconds

    @Parameter(names = "-host", description = "hostname of apiserver to monitor", required = true)
    static String HOST;

    @Parameter(names = "-interval", description = "how often to check apiserver", required = true)
    static long POLLING_INTERVAL;

    private  LoginStats getLoginStatus(String host) {
        RestClient restClient = new RestClient();
        return restClient.login(host);

    }

    public static void main(String[] args) throws Exception {
        Monitor m = new Monitor();
        JCommander.newBuilder()
                .addObject(m)
                .build()
                .parse(args);

        if (HOST.isEmpty()) {
            System.err.println("Please provide a host via -host");
            exit(1);
        }

        final Connection connection = DriverManager.getConnection("ojai:mapr:");
        if (connection == null) {
            System.err.println("failed to connect to ojai");
            exit(1);
        }
        DocumentStore store = connection.getStore(TABLE_PATH);
        if (store == null)
            store = connection.createStore(TABLE_PATH);

        int retry = 0;
        boolean emailSent = false;
        long timeSent = System.currentTimeMillis() / 1000;;
        try {
            while (true) {
                LoginStats loginStats = m.getLoginStatus(HOST);
                Document status = connection.newDocument(loginStats);
                System.out.println("\t inserting " + status.getId());
                // insert the OJAI Document into the DocumentStore
                store.insertOrReplace(status);

                if (retry < 20) {
                    if (loginStats.statusCode != 200 && loginStats.statusCode != 201) {
                        retry++;
                    } else
                        retry = 0;
                } else {

                    if (loginStats.statusCode == 200 || loginStats.statusCode == 201) {
                        retry = 0;
                        emailSent = false;
                        continue;
                    }

                    long now = System.currentTimeMillis() / 1000;
                    // send email every 4 hours and may be trigger a redeploy job
                    if (!emailSent) {
                        // send an email
                        emailSent = true;
                        timeSent = System.currentTimeMillis() / 1000;
                        System.out.println(HOST + " is down, please check");
                    } else {
                        if ((now - timeSent) > 4 * 3600) {
                            timeSent = System.currentTimeMillis() / 1000;
                            System.out.println(HOST + " is down, please check");
                        }
                    }

                }

                Thread.sleep(POLLING_INTERVAL);

            }
        } finally {
            connection.close();
        }
    }


}
