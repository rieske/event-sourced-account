package lt.rieske.accounts;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

class AsyncAccountClient {

    private final CloseableHttpAsyncClient httpClient;
    private final String apiUrl;

    public AsyncAccountClient(String apiUrl) {
        this.apiUrl = apiUrl;
        var requestConfig = RequestConfig.custom()
                .setSocketTimeout(3000)
                .setConnectTimeout(3000)
                .build();
        this.httpClient = HttpAsyncClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(16)
                .setMaxConnTotal(256)
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
                    @Override
                    public long getKeepAliveDuration(org.apache.http.HttpResponse response, HttpContext context) {
                        long duration = super.getKeepAliveDuration(response, context);
                        return duration > -1 ? duration : 5_000;
                    }
                })
                .build();
        this.httpClient.start();
    }

    void close() throws IOException {
        this.httpClient.close();
    }

    void openAccount(UUID accountId, UUID ownerId, CompletableFuture<HttpResponse> future) {
        httpPost("/account/" + accountId + "?owner=" + ownerId, future);
    }

    void queryAccount(UUID accountId, CompletableFuture<HttpResponse> future) {
        httpGet("/account/" + accountId, future);
    }

    void deposit(UUID accountId, int amount, CompletableFuture<HttpResponse> future) {
        Supplier<HttpUriRequest> request = () -> {
            var txId = UUID.randomUUID();
            String path = String.format("/account/%s/deposit?amount=%d&transactionId=%s", accountId, amount, txId);
            return new HttpPut(apiUrl + path);
        };
        httpClient.execute(request.get(), new HttpResponseFutureCallback(future, request));
    }

    void deposit(UUID accountId, int amount, UUID txId, CompletableFuture<HttpResponse> future) {
        httpPut("/account/" + accountId + "/deposit?amount=" + amount + "&transactionId=" + txId, future);
    }

    private void httpGet(String path, CompletableFuture<HttpResponse> future) {
        var request = new HttpGet(apiUrl + path);
        httpClient.execute(request, new HttpResponseFutureCallback(future, () -> request));
    }

    private void httpPost(String path, CompletableFuture<HttpResponse> future) {
        var request = new HttpPost(apiUrl + path);
        httpClient.execute(request, new HttpResponseFutureCallback(future, () -> request));
    }

    private void httpPut(String path, CompletableFuture<HttpResponse> future) {
        var request = new HttpPut(apiUrl + path);
        httpClient.execute(request, new HttpResponseFutureCallback(future, () -> request));
    }

    private static String read(InputStream inputStream) {
        var textBuilder = new StringBuilder();
        try (var reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return textBuilder.toString();
    }

    public static record HttpResponse(int code, String body, int conflicts) {
    }

    private class HttpResponseFutureCallback implements FutureCallback<org.apache.http.HttpResponse> {
        private final CompletableFuture<HttpResponse> future;
        private final Supplier<HttpUriRequest> request;
        private int conflicts = 0;

        HttpResponseFutureCallback(CompletableFuture<HttpResponse> future, Supplier<HttpUriRequest> request) {
            this.future = future;
            this.request = request;
        }

        @Override
        public void completed(org.apache.http.HttpResponse result) {
            try {
                var entity = result.getEntity();
                String body = null;
                if (entity != null) {
                    body = read(entity.getContent());
                }
                EntityUtils.consume(entity);
                int statusCode = result.getStatusLine().getStatusCode();
                if (statusCode == 409) {
                    conflicts++;
                    httpClient.execute(request.get(), this);
                } else {
                    future.complete(new HttpResponse(statusCode, body, conflicts));
                }
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
        }

        @Override
        public void failed(Exception e) {
            future.completeExceptionally(e);
        }

        @Override
        public void cancelled() {
            future.cancel(false);
        }
    }
}
