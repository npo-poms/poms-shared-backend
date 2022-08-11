package nl.vpro.semantic;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.SneakyThrows;

import java.time.Duration;
import java.util.concurrent.Future;

import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import nl.vpro.jackson2.Jackson2Mapper;

import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;


/**
 * Implementation of {@link VectorizationService}
 */
public class ApiVectorizationServiceImpl implements VectorizationService {

    private final CloseableHttpAsyncClient client;

    private final String apiKey;

    private final String endPoint;

    private final MeterRegistry meterRegistry;



    public ApiVectorizationServiceImpl(
        String endpoint,
        String apiKey,
        MeterRegistry meterRegistry,
        Duration timeout) {
        this.apiKey = apiKey;
        this.endPoint = endpoint == null? "https://www.api.geniusvoicedemo.nl/semanticvectorizer" : endpoint;
        this.meterRegistry = meterRegistry;
        final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
            .setSoTimeout(Timeout.ofMilliseconds(timeout.toMillis()))
            .build();

        client = HttpAsyncClients.custom()
            .setIOReactorConfig(ioReactorConfig)
            .setRetryStrategy(
                new DefaultHttpRequestRetryStrategy(5, TimeValue.ofSeconds(5))
        )
            .build();
        client.start();
    }


    @SneakyThrows
    @Override
    public float[] forQuery(String query) {
        meterRegistry.counter("vectorization", "for", "query").increment();
        return post(new Query(query)).getEmbedding();
    }

    @Override
    public float[] forText(String text) {
        meterRegistry.counter("vectorization", "for", "text").increment();
        return post(new Description(text)).getEmbedding();
    }


    @SneakyThrows
    protected Response post(Object body) {
        final SimpleHttpRequest request = SimpleRequestBuilder.post(endPoint)
            .addHeader(new BasicHeader("api_key", apiKey))
            .addHeader(new BasicHeader("accept", APPLICATION_JSON))
            .addHeader(new BasicHeader("content-type", APPLICATION_JSON))
            .setBody(Jackson2Mapper.getInstance().writeValueAsBytes(body), APPLICATION_JSON)
            .build();

        final Future<SimpleHttpResponse> execute = client.execute(
            SimpleRequestProducer.create(request),
            SimpleResponseConsumer.create(), null);

        SimpleBody responseBody = execute.get().getBody();
        return Jackson2Mapper
            .getLenientInstance()
            .readerFor(Response.class)
            .readValue(responseBody.getBodyBytes());
    }

    @Data
    public static class Query {

        private final String query;

        Query(String query) {
            this.query = query;
        }
    }

    @Data
    public static class Description {

        private final String description;

        Description(String description) {
            this.description = description;
        }
    }


    @Data
    public static class Response {
        private float[] embedding;
    }

}
