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
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.System.exit;


public class Monitor {
    static Logger logger = LoggerFactory.getLogger(Monitor.class);

    @Parameter(names = "-host", description = "hostname of apiserver to monitor", required = true)
    static String host;

    @Parameter(names = "-reset", description = "passing this flag to reset monitoring data at beginning of weekly run", required = false)
    static boolean reset = false;


    @Parameter(names = "-email", description = "gmail for receiving alert", required = false)
    static String to;

    @Parameter(names = "-email-password", description = "your gmail account password", password = false)
    static String password = null;

    @Parameter(names = "-interval", description = "how often to check apiserver", hidden = true)
    static long polling_interval = 60000;

    @Parameter(names = "-help", description = "display this help", help = true)
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

        if (reset) {
            ES.reset();
            exit(0);
        }

        System.out.println("monitoring " + host + " ...");
        logger.info("monitoring apiserver on " + host);
        logger.info("alert email is set to " + to);

        int retry = 0;
        boolean emailSent = false;
        long timeSent = System.currentTimeMillis() / 1000L;

        try {
            while (true) {
                LoginStats loginStats = m.getLoginStatus(host);
                ES.insert(loginStats);

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
                        SendMail.sendmail(to, password, loginStats.getMsg());
                        emailSent = true;
                        timeSent = System.currentTimeMillis() / 1000L;
                        Date date = new Date(timeSent);
                        logger.info(date.toString() + " email sent " + loginStats.getMsg());
                    } else {
                        // send email every 4 hours
                        if ((now - timeSent) > 4 * 3600) {
                            SendMail.sendmail(to, password, loginStats.getMsg());
                            timeSent = System.currentTimeMillis() / 1000L;
                            Date date = new Date(timeSent);
                            logger.info(date.toString() + " email sent" + loginStats.getMsg());
                        }
                    }
                }

                Thread.sleep(polling_interval);

            }
        } finally {
            ES.close();
        }
    }


}
