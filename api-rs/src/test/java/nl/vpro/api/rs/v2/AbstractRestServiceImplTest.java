package nl.vpro.api.rs.v2;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;

import org.custommonkey.xmlunit.XMLUnit;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.junit.Before;

import nl.vpro.jackson.ObjectMapper;
import nl.vpro.resteasy.JacksonContextResolver;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public abstract class AbstractRestServiceImplTest {
    public static final MediaType JSON;

    public static final MediaType XML;

    static {
        Map<String, String> params = new HashMap<>();
        JSON = new MediaType("application", "json", params);
        XML = new MediaType("application", "xml", params);
    }

    protected ObjectMapper mapper;


    protected Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();

    @Before
    public void setup() throws Exception {
        ContextResolver<org.codehaus.jackson.map.ObjectMapper> contextResolver = new JacksonContextResolver();
        dispatcher.getProviderFactory().registerProviderInstance(contextResolver);
        mapper = ObjectMapper.INSTANCE;
        dispatcher.getRegistry().addSingletonResource(getTestObject());


        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);

    }

    protected abstract Object getTestObject();
}
