package com.mapr.qa;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.io.IOException;

import static java.lang.System.exit;


public class Monitor {
    static Logger logger = LoggerFactory.getLogger(Monitor.class);
    static String TABLE_PATH = "/user/mapr/mcs/uptime";
    //static int POLLING_INTERVAL = 60000; //1 min in miliseconds
    static final String INDEX = "mcsmonitor";

    @Parameter(names = "-host", description = "hostname of apiserver to monitor", required = true)
    static String host;

    @Parameter(names = "-email", description = "gmail for receiving alert", required = true)
    static String to;

    @Parameter(names = "-email-password", description = "your gmail account password", password = true)
    static String password = null;

    @Parameter(names = "-interval", description = "how often to check apiserver", hidden = true)
    static long polling_interval = 10000;

    @Parameter(names = "--help", help = true)
    static boolean help = false;

    private LoginStats getLoginStatus(String host) throws Exception {
        MyRestClient restClient = new MyRestClient();
        return restClient.login(host);

    }

    public static void main(String[] args) throws Exception {
        Monitor m = new Monitor();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(m)
                .build();
        jCommander.parse(args);

        if (help) {
            jCommander.usage();
            exit(0);
        }

        System.out.println("monitoring " + host + " ...");
        logger.info("monitoring apiserver on " + host);
        logger.info("alert email is set to " + to);

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
                LoginStats loginStats = m.getLoginStatus(host);
                //write to elasticsearch
                String id = RandomStringUtils.randomAlphanumeric(20);
                IndexRequest indexRequest = new IndexRequest(INDEX, "_doc", id);
                String jstring = new Gson().toJson(loginStats);
                IndexResponse response = esclient.index(indexRequest.source(jstring, XContentType.JSON), RequestOptions.DEFAULT);
                if (DocWriteResponse.Result.CREATED != response.getResult()) {
                    logger.error("index creation failed for doc " + jstring + " error:" + response.getResult());
                    new Exception("index creation failed for doc " + jstring + " error:" + response.getResult());
                } else
                    logger.info("index creation succeeded for doc " + jstring);

                // retry for 20 min
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
                    // send email every 4 hours
                    if (!emailSent) {
                        // todo send an email
                        logger.info("sending email " + loginStats.getMsg());
                        SendMail.sendmail(to, password, loginStats.getMsg());
                        emailSent = true;
                        timeSent = System.currentTimeMillis() / 1000L;
                    } else {
                        // send email every 4 hours
                        if ((now - timeSent) > 4 * 3600) {
                            logger.info("sending email " + loginStats.getMsg());
                            SendMail.sendmail(to, password, loginStats.getMsg());
                            timeSent = System.currentTimeMillis() / 1000L;
                        }
                    }
                }

                Thread.sleep(polling_interval);

            }
        } finally {
            esclient.close();
        }
    }


}
