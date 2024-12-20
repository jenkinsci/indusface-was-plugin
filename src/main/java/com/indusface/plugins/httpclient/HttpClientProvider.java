package com.indusface.plugins.httpclient;

import hudson.ProxyConfiguration;
import java.net.http.HttpClient;

/**
 * A singleton class to provide a single instance of HttpClient for use in the
 * application.
 */
public class HttpClientProvider {

    // Singleton HttpClient instance
    private static final HttpClient client =
            ProxyConfiguration.newHttpClientBuilder().build();

    /**
     * Get the singleton instance of HttpClient.
     *
     * @return the HttpClient instance
     */
    public static HttpClient getHttpClient() {
        return client;
    }
}
