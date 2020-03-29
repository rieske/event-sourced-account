package lt.rieske.accounts;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
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

import static org.assertj.core.api.Assertions.assertThat;

public class AccountClient {

    private final CloseableHttpClient httpClient;
    private final String apiUrl;

    public AccountClient(String apiUrl) {
        this.apiUrl = apiUrl;
        var connectionManager = new PoolingHttpClientConnectionManager();
        this.httpClient = HttpClientBuilder
                .create()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
                    @Override
                    public long getKeepAliveDuration(org.apache.http.HttpResponse response, HttpContext context) {
                        long duration = super.getKeepAliveDuration(response, context);
                        return duration > -1 ? duration : 5_000;
                    }
                })
                .build();
    }

    public void openAccount(UUID accountId, UUID ownerId) {
        var response = httpPost("/account/" + accountId + "?owner=" + ownerId);
        assertThat(response.code).isEqualTo(201);
    }

    public String queryAccount(UUID accountId) {
        var response = httpGet("/account/" + accountId);
        assertThat(response.code).isEqualTo(200);
        return response.body;
    }

    public int deposit(UUID accountId, int amount, UUID txId) {
        var response = httpPut("/account/" + accountId + "/deposit?amount=" + amount + "&transactionId=" + txId);
        return response.code;
    }

    private HttpResponse httpGet(String path) {
        return executeHttpRequest(new HttpGet(apiUrl + path));
    }

    private HttpResponse httpPost(String path) {
        return executeHttpRequest(new HttpPost(apiUrl + path));
    }

    private HttpResponse httpPut(String path) {
        return executeHttpRequest(new HttpPut(apiUrl + path));
    }

    private HttpResponse executeHttpRequest(HttpUriRequest request) {
        try (var response = httpClient.execute(request)) {
            var entity = response.getEntity();
            String body = null;
            if (entity != null) {
                body = read(entity.getContent());
            }
            EntityUtils.consume(entity);
            return new HttpResponse(response.getStatusLine().getStatusCode(), body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(InputStream inputStream) {
        var textBuilder = new StringBuilder();
        try (var reader = new BufferedReader(new InputStreamReader
                (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return textBuilder.toString();
    }

    public static class HttpResponse {
        private final int code;
        private final String body;

        public HttpResponse(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }
}
