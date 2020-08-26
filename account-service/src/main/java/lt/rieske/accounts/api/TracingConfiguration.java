package lt.rieske.accounts.api;

import brave.Tracing;
import brave.sampler.Sampler;
import brave.sparkjava.SparkTracing;
import com.p6spy.engine.spy.P6DataSource;
import spark.ExceptionHandler;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;

import static spark.Spark.afterAfter;
import static spark.Spark.before;

public interface TracingConfiguration {
    void init();
    void closeResources();

    <T extends Exception> ExceptionHandler<? super T> exception(ExceptionHandler<? super T> handler);
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
    public void init() {
    }

    @Override
    public void closeResources() {
    }

    @Override
    public <T extends Exception> ExceptionHandler<? super T> exception(ExceptionHandler<? super T> handler) {
        return handler;
    }

    @Override
    public DataSource decorate(DataSource dataSource) {
        return dataSource;
    }
}

class ZipkinTracingConfiguration implements TracingConfiguration {

    private final Sender sender;
    private final AsyncReporter<Span> spanReporter;
    private final SparkTracing sparkTracing;

    public ZipkinTracingConfiguration(String zipkinUrl) {
        this.sender = URLConnectionSender.create(zipkinUrl);
        this.spanReporter = AsyncReporter.create(sender);

        var tracing = Tracing.newBuilder()
                .localServiceName("account")
                .sampler(Sampler.NEVER_SAMPLE)
                .spanReporter(spanReporter)
                .build();
        this.sparkTracing = SparkTracing.create(tracing);
    }

    @Override
    public void init() {
        before(sparkTracing.before());
        afterAfter(sparkTracing.afterAfter());
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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Exception> ExceptionHandler<T> exception(ExceptionHandler<? super T> handler) {
        return sparkTracing.exception(handler);
    }

    @Override
    public DataSource decorate(DataSource dataSource) {
        return new P6DataSource(dataSource);
    }
}