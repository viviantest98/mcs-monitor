package com.mapr.qa;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.mapr.db.MapRDB;
import com.mapr.db.Table;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.json.JSONObject;
import org.ojai.Document;
import org.ojai.store.Connection;
import org.ojai.store.DocumentStore;
import org.ojai.store.DriverManager;

import java.io.IOException;

import static java.lang.System.exit;


public class Monitor {
    static String TABLE_PATH = "/user/mapr/mcs/uptime";
    //static int POLLING_INTERVAL = 60000; //1 min in miliseconds
    static final String INDEX = "mcsmonitor";

    @Parameter(names = "-host", description = "hostname of apiserver to monitor", required = true)
    static String HOST;

    @Parameter(names = "-interval", description = "how often to check apiserver", required = true)
    static long POLLING_INTERVAL;

    private LoginStats getLoginStatus(String host) {
        MyRestClient restClient = new MyRestClient();
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

        if (POLLING_INTERVAL == 0) {
            System.err.println("Please provide a polling interval via -interval");
            exit(1);
        }

        // initialize index
        RestHighLevelClient esclient = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("10.10.100.104", 9200, "http"))
                        .setRequestConfigCallback(requestConfigBuilder ->
                                requestConfigBuilder
                                        .setConnectTimeout(5000)
                                        .setSocketTimeout(30000)));


        int retry = 0;
        boolean emailSent = false;
        long timeSent = System.currentTimeMillis() / 1000L;

        try {
            while (true) {
                LoginStats loginStats = m.getLoginStatus(HOST);
                //write to elasticsearch
                String id = RandomStringUtils.randomAlphanumeric(20);
                IndexRequest indexRequest = new IndexRequest(INDEX, "_doc", id);
                String jstring = new Gson().toJson(loginStats);
                IndexResponse response = esclient.index(indexRequest.source(jstring, XContentType.JSON), RequestOptions.DEFAULT);
                if (DocWriteResponse.Result.CREATED != response.getResult())
                    System.out.println("index creation failed for doc " + jstring + " error:" + response.getResult());
                else
                    System.out.println("index creation succeeded for doc " + jstring);

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

                    long now = System.currentTimeMillis() / 1000L;
                    // send email every 4 hours and may be trigger a redeploy job
                    if (!emailSent) {
                        // send an email
                        emailSent = true;
                        timeSent = System.currentTimeMillis() / 1000L;
                        System.out.println(HOST + " is down, please check");
                    } else {
                        if ((now - timeSent) > 4 * 3600) {
                            timeSent = System.currentTimeMillis() / 1000L;
                            System.out.println(HOST + " is down, please check");
                        }
                    }

                }

                Thread.sleep(POLLING_INTERVAL);

            }
        } finally {
            esclient.close();
        }
    }


}
