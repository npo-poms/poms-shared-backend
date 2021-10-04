package nl.vpro.semantic;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.SneakyThrows;

import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;

import nl.vpro.jackson2.Jackson2Mapper;

import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;


/**
 * Implementation of {@link VectorizationService}
 */
public class ApiVectorizationServiceImpl implements VectorizationService {


    private final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(5))
                .build();

    private final CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setIOReactorConfig(ioReactorConfig)
                .build();

    private final String apiKey;

    private final String endPoint;

    @Inject
    private MeterRegistry meterRegistry;


    public ApiVectorizationServiceImpl(String endpoint, String apiKey, MeterRegistry meterRegistry) {
        this.apiKey = apiKey;
        this.endPoint = endpoint == null? "https://www.api.geniusvoicedemo.nl/semanticvectorizer" : endpoint;
        this.meterRegistry = meterRegistry;
        client.start();
    }


    @SneakyThrows
    @Override
    public float[] forQuery(String query) {
        meterRegistry.counter("vectorization", "for", "query");
        return post(new Query(query)).getEmbedding();
    }

    @Override
    public float[] forText(String text) {
        meterRegistry.counter("vectorization", "for", "text");
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

        Future<SimpleHttpResponse> execute = client.execute(
            SimpleRequestProducer.create(request),
            SimpleResponseConsumer.create(), null);

        SimpleBody responseBody = execute.get().getBody();
        return Jackson2Mapper.getLenientInstance().readerFor(Response.class).readValue(responseBody.getBodyBytes());
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
