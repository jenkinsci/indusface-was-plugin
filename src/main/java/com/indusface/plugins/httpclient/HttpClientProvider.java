package com.indusface.plugins.httpclient;

import hudson.ProxyConfiguration;
import java.io.IOException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

/**
 * A singleton class to provide a single instance of HttpClient for use in the application.
 */
public class HttpClientProvider {

    /**
     * Get the singleton instance of HttpClient.
     *
     * @return the HttpClient instance
     */
    public static CloseableHttpClient getHttpClient() {

        Jenkins jenkins = Jenkins.getInstanceOrNull();
        ProxyConfiguration proxyConfig = jenkins != null ? jenkins.proxy : null;

        if (proxyConfig == null) {
            return HttpClients.createDefault();
        }
        HttpHost proxy = new HttpHost(proxyConfig.name, proxyConfig.port);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (proxyConfig.getUserName() != null && proxyConfig.getSecretPassword() != null) {
            credentialsProvider.setCredentials(
                    new AuthScope(proxyConfig.name, proxyConfig.port),
                    new UsernamePasswordCredentials(
                            proxyConfig.getUserName(), proxyConfig.getPassword().toCharArray()));
        }

        return HttpClients.custom()
                .setProxy(proxy)
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }

    public static JSONObject parseResponse(HttpEntity entity) throws IOException, ParseException {
        return JSONObject.fromObject(EntityUtils.toString(entity));
    }
}
