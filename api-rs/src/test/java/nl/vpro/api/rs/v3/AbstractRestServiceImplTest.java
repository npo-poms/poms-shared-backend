package nl.vpro.api.rs.v3;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;

import org.custommonkey.xmlunit.XMLUnit;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.junit.Before;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.api.rs.v3.validation.ScheduleFormValidatingReader;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.resteasy.JacksonContextResolver;

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
        ScheduleFormValidatingReader reader = new ScheduleFormValidatingReader();
        reader.setDoValidate(true);
        reader.init();
        dispatcher.getProviderFactory().register(reader);
        mapper = Jackson2Mapper.INSTANCE;
        dispatcher.getRegistry().addSingletonResource(getTestObject());


        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
    }

    protected abstract T getTestObject();
}
