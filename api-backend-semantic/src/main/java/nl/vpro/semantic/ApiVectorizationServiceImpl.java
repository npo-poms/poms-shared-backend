package nl.vpro.semantic;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import lombok.*;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

import org.checkerframework.checker.nullness.qual.Nullable;

import nl.vpro.jackson2.Jackson2Mapper;


/**
 * Implementation of {@link VectorizationService}
 */
public class ApiVectorizationServiceImpl implements VectorizationService {

    private static final Jackson2Mapper MAPPER = Jackson2Mapper.getInstance();

    protected static final Jackson2Mapper LENIENT = Jackson2Mapper.getLenientInstance();


    private final HttpClient client;


    private final String apiKey;

    private final String endPoint;

    private final MeterRegistry meterRegistry;

    private final Duration timeout;


    public ApiVectorizationServiceImpl(
        @Nullable String endpoint,
        @NonNull String apiKey,
        @Nullable MeterRegistry meterRegistry,
        Duration timeout) {
        this.apiKey = apiKey;
        this.endPoint = endpoint == null? "https://www.api.geniusvoicedemo.nl/semanticvectorizer" : endpoint;
        this.meterRegistry = meterRegistry == null ? new LoggingMeterRegistry() : meterRegistry;
        client = HttpClient.newBuilder()
            .connectTimeout(timeout)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();
        this.timeout = timeout;
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
        var request = HttpRequest.newBuilder(URI.create(endPoint))
            .POST(HttpRequest.BodyPublishers.ofByteArray(MAPPER.writeValueAsBytes(body)))
            .header("api_key", apiKey)
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .build();

        final HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        return LENIENT
            .readerFor(Response.class)
            .readValue(response.body());
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
