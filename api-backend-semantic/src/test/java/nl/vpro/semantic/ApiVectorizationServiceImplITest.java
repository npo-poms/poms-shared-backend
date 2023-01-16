package nl.vpro.semantic;

import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


@Log4j2
class ApiVectorizationServiceImplITest {



    @Test
    void forQuery() {
        float[] vector = getImpl().forQuery("hallo daar");

        assertThat(vector).hasSize(512);

    }

    @Test
    void forText() {
        float[] vector = getImpl().forText("hallo daar");

        assertThat(vector).hasSize(512);

    }

    ApiVectorizationServiceImpl getImpl() {
        return new ApiVectorizationServiceImpl(
            null,
            "70ccf7c8-4dbc-4711-be57-a51a8a971cb3",
            new LoggingMeterRegistry(),
            Duration.ofSeconds(10)
        );

    }
}
