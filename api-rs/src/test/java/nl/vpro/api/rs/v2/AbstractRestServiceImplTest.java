package nl.vpro.api.rs.v2;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;

import org.custommonkey.xmlunit.XMLUnit;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.junit.Before;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.domain.media.bind.Jackson2Mapper;
import nl.vpro.resteasy.JacksonContextResolver;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public abstract class AbstractRestServiceImplTest {
    public static final MediaType JSON;

    public static final MediaType XML;

    static {
        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put("charset", "utf-8");

        JSON = new MediaType("application", "json", parameters);
        XML = new MediaType("application", "xml", parameters);
    }

    protected ObjectMapper mapper;

    protected Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();

    @Before
    public void setup() throws Exception {
        ContextResolver<ObjectMapper> contextResolver = new JacksonContextResolver();
        dispatcher.getProviderFactory().registerProviderInstance(contextResolver);
        mapper = Jackson2Mapper.INSTANCE;
        dispatcher.getRegistry().addSingletonResource(getTestObject());

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
    }

    protected abstract Object getTestObject();
}
