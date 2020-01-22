package com.mapr.qa;

import okhttp3.*;
import org.apache.commons.lang3.RandomStringUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

public class RestClient {

    public LoginStats login(String host) {
        LoginStats loginStats;
        Request request = new Request.Builder()
                .url("https://" + host + ":8443/login/")
                .post(new FormBody.Builder()
                        .add("username", "mapr")
                        .add("password", "mapr")
                        .build())
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            loginStats = new LoginStats(RandomStringUtils.randomAlphanumeric(20), response.code(), System.currentTimeMillis() / 1000);
        } catch (IOException e) {
            e.printStackTrace();
            loginStats = null;
        } finally {
            response.close();
        }
        return loginStats;
    }

    OkHttpClient client = new OkHttpClient.Builder().
            connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager) trustAllCerts[0])
            .hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            })
            .build();

    private static final TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };
    private static final SSLContext trustAllSslContext;

    static {
        try {
            trustAllSslContext = SSLContext.getInstance("SSL");
            trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();
}
