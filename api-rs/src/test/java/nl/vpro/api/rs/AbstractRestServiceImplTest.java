package nl.vpro.api.rs;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.BeforeEach;


import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.api.rs.exception.*;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.jackson2.rs.JacksonContextResolver;
import nl.vpro.rs.converters.DateParamConverterProvider;
import nl.vpro.rs.converters.LocaleParamConverterProvider;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public abstract class AbstractRestServiceImplTest<T> {
    public static final MediaType JSON;

    public static final MediaType XML;

    static {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("charset", "utf-8");

        JSON = new MediaType("application", "json", parameters);
        XML = new MediaType("application", "xml", parameters);
    }

    protected ObjectMapper mapper;

    protected final Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();


    @BeforeEach
    public void setup() {
        ContextResolver<ObjectMapper> contextResolver = new JacksonContextResolver();
        dispatcher.getProviderFactory().registerProviderInstance(contextResolver);
        dispatcher.getProviderFactory().registerProvider(DateParamConverterProvider.class);
        dispatcher.getProviderFactory().registerProvider(LocaleParamConverterProvider.class);
        dispatcher.getProviderFactory().registerProvider(NotFoundExceptionMapper.class);
        dispatcher.getProviderFactory().registerProvider(NotFoundInProfileExeptionMapper.class);
        dispatcher.getProviderFactory().registerProvider(ProfileNotFoundExceptionMapper.class);


        mapper = Jackson2Mapper.getInstance();
        dispatcher.getRegistry().addSingletonResource(getTestObject());

    }

    protected abstract T getTestObject();


    protected void assert200(MockHttpResponse response) {
        try {
            assertThat(response.getStatus()).withFailMessage(response.getErrorMessage() + " " + response.getContentAsString()).isEqualTo(200);
        } catch (UnsupportedEncodingException use) {
            throw new RuntimeException(use);
        }
    }

    protected void assert400(MockHttpResponse response) {
        try {
            assertThat(response.getStatus()).withFailMessage(response.getErrorMessage() + " " + response.getContentAsString()).isEqualTo(400);
        } catch (UnsupportedEncodingException use) {
            throw new RuntimeException(use);
        }
    }


    protected AbstractCharSequenceAssert<?, String> assert404(MockHttpResponse response) {
        try {
            String result = response.getContentAsString();
            assertThat(response.getStatus()).withFailMessage(
                response.getErrorMessage() + " " + response.getContentAsString(), response.getStatus()).isEqualTo(404);
            return assertThat(result);
        } catch (UnsupportedEncodingException use) {
            throw new RuntimeException(use);
        }
    }
}
