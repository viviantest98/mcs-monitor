package com.mapr.qa;

import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ES {
    static final String INDEX = "mcsmonitor";
    static private RestHighLevelClient esclient;
    static Logger logger = LoggerFactory.getLogger(ES.class);

    static void getInstance() {
        // initialize index
        esclient = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("10.10.100.104", 9200, "http"))
                        .setRequestConfigCallback(requestConfigBuilder ->
                                requestConfigBuilder
                                        .setConnectTimeout(5000)
                                        .setSocketTimeout(30000)));
    }

    static void reset() throws Exception {
        logger.info("reset data");
        if (esclient == null)
            getInstance();
        DeleteByQueryRequest request = new DeleteByQueryRequest(INDEX);
        request.setQuery(QueryBuilders.matchAllQuery());
        BulkByScrollResponse response = esclient.deleteByQuery(request, RequestOptions.DEFAULT);
        logger.info(response.getStatus().toString());

    }

    static void insert(LoginStats loginStats) throws Exception{
        String id = RandomStringUtils.randomAlphanumeric(20);
        IndexRequest indexRequest = new IndexRequest(INDEX, "_doc", id);
        String jstring = new Gson().toJson(loginStats);
        if (esclient == null)
            getInstance();
        IndexResponse response = esclient.index(indexRequest.source(jstring, XContentType.JSON), RequestOptions.DEFAULT);
        if (DocWriteResponse.Result.CREATED != response.getResult()) {
            logger.error("index creation failed for doc " + jstring + " error:" + response.getResult());
            new Exception("index creation failed for doc " + jstring + " error:" + response.getResult());
        }
    }

    static void close() throws Exception {
        esclient.close();
    }
}
