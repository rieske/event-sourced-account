package lt.rieske.accounts.infrastructure;

import com.p6spy.engine.spy.P6DataSource;
import io.helidon.tracing.Tracer;
import io.helidon.tracing.TracerBuilder;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

public interface TracingConfiguration {
    Tracer tracer();

    void closeResources();

    DataSource decorate(DataSource dataSource);

    static TracingConfiguration create(String zipkinUrl) {
        return zipkinUrl == null ? noop() : new ZipkinTracingConfiguration(zipkinUrl);
    }

    static TracingConfiguration noop() {
        return new NoOpTracingConfiguration();
    }
}

class NoOpTracingConfiguration implements TracingConfiguration {
    @Override
    public Tracer tracer() {
        return Tracer.noOp();
    }

    @Override
    public void closeResources() {
    }

    @Override
    public DataSource decorate(DataSource dataSource) {
        return dataSource;
    }
}

class ZipkinTracingConfiguration implements TracingConfiguration {

    private final String zipkinUrl;
    private final Sender sender;
    private final AsyncReporter<Span> spanReporter;

    public ZipkinTracingConfiguration(String zipkinUrl) {
        this.zipkinUrl = zipkinUrl;
        this.sender = URLConnectionSender.create(zipkinUrl);
        this.spanReporter = AsyncReporter.create(sender);
    }

    @Override
    public Tracer tracer() {
        return TracerBuilder.create("account")
                .collectorUri(URI.create(zipkinUrl))
                .build();
    }

    @Override
    public void closeResources() {
        try {
            spanReporter.close();
            sender.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public DataSource decorate(DataSource dataSource) {
        return new P6DataSource(dataSource);
    }
}