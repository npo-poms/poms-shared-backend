package nl.vpro.api.rs.v3;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;

import org.custommonkey.xmlunit.XMLUnit;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.resteasy.DateParamConverterProvider;
import nl.vpro.resteasy.JacksonContextResolver;
import nl.vpro.resteasy.LocaleParamConverterProvider;

import static org.junit.Assert.assertEquals;

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


    @Before
    public void setup() throws Exception {
        ContextResolver<ObjectMapper> contextResolver = new JacksonContextResolver();
        dispatcher.getProviderFactory().registerProviderInstance(contextResolver);
        dispatcher.getProviderFactory().registerProvider(DateParamConverterProvider.class);
        dispatcher.getProviderFactory().registerProvider(LocaleParamConverterProvider.class);

        mapper = Jackson2Mapper.INSTANCE;
        dispatcher.getRegistry().addSingletonResource(getTestObject());


        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
    }

    protected abstract T getTestObject();


    protected void assert200(MockHttpResponse response) {
        assertEquals(response.getErrorMessage() + " " + response.getContentAsString(), 200, response.getStatus());
    }

    protected void assert200(MockHttpServletResponse response) throws UnsupportedEncodingException {
        assertEquals(response.getErrorMessage() + " " + response.getContentAsString(), 200, response.getStatus());
    }

    protected void assert400(MockHttpResponse response) {
        assertEquals(response.getErrorMessage() + " " + response.getContentAsString(), 400, response.getStatus());
    }

}
