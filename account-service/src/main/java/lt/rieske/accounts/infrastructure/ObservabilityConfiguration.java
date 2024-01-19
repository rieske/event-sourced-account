package lt.rieske.accounts.infrastructure;

import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import com.p6spy.engine.spy.P6DataSource;
import io.micrometer.context.ContextRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.transport.RequestReplyReceiverContext;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.W3CPropagation;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import spark.Request;
import spark.Response;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;

public interface ObservabilityConfiguration {
    void closeResources();

    DataSource decorate(DataSource dataSource);

    Observation startApiOperationObservation(Request request, Response response);

    static ObservabilityConfiguration create(MeterRegistry meterRegistry, String zipkinUrl) {
        return zipkinUrl == null ? noop() : new ZipkinTracingConfiguration(meterRegistry, zipkinUrl);
    }

    static ObservabilityConfiguration noop() {
        return new NoOpObservabilityConfiguration();
    }
}

class NoOpObservabilityConfiguration implements ObservabilityConfiguration {
    @Override
    public void closeResources() {
    }

    @Override
    public DataSource decorate(DataSource dataSource) {
        return dataSource;
    }

    @Override
    public Observation startApiOperationObservation(Request request, Response response) {
        return Observation.NOOP;
    }
}

class ZipkinTracingConfiguration implements ObservabilityConfiguration {
    private static final String API_OPERATION_TAG = "api_operation";

    private final Sender sender;
    private final AsyncReporter<zipkin2.Span> spanReporter;
    private final ObservationRegistry observationRegistry;

    static class W3CPropagationBridge extends W3CPropagation {

        @Override
        public Propagation<String> get() {
            return create(KeyFactory.STRING);
        }
    }

    public ZipkinTracingConfiguration(MeterRegistry meterRegistry, String zipkinUrl) {
        this.sender = URLConnectionSender.create(zipkinUrl);
        this.spanReporter = AsyncReporter.create(sender);

        ThreadLocalCurrentTraceContext braveCurrentTraceContext = ThreadLocalCurrentTraceContext.newBuilder()
                .addScopeDecorator(MDCScopeDecorator.get())
                .build();

        CurrentTraceContext bridgeContext = new BraveCurrentTraceContext(braveCurrentTraceContext);

        Tracing tracing = Tracing.newBuilder()
                .localServiceName(System.getenv("SERVICE_NAME"))
                .sampler(Sampler.NEVER_SAMPLE)
                .currentTraceContext(braveCurrentTraceContext)
                .supportsJoin(false)
                .traceId128Bit(true)
                // TODO: BaggageManager in W3CPropagation
                .propagationFactory(new W3CPropagationBridge())
                .addSpanHandler(ZipkinSpanHandler.create(spanReporter))
                .build();

        Tracer tracer = new BraveTracer(tracing.tracer(), bridgeContext);

        ContextRegistry.getInstance().registerThreadLocalAccessor(new ObservationAwareSpanThreadLocalAccessor(tracer));

        Propagator propagator = new BravePropagator(tracing);
        this.observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig()
                .observationHandler(new TracingAwareMeterObservationHandler<>(new DefaultMeterObservationHandler(meterRegistry), tracer))
                .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
                        new PropagatingSenderTracingObservationHandler<>(tracer, propagator),
                        new PropagatingReceiverTracingObservationHandler<>(tracer, propagator),
                        new DefaultTracingObservationHandler(tracer))
                );

        meterRegistry.config().meterFilter(new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().equals(API_OPERATION_TAG)) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .build()
                            .merge(config);
                }
                return config;
            }
        });
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

    @Override
    public Observation startApiOperationObservation(Request request, Response response) {
        return Observation.start(API_OPERATION_TAG, () -> {
            RequestReplyReceiverContext<Request, Response> context = new RequestReplyReceiverContext<>(Request::headers);
            context.setCarrier(request);
            context.setResponse(response);
            return context;
        }, observationRegistry);
    }
}