package nl.vpro.api.rs.v2.media;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.media.MediaBuilder;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.search.MediaForm;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.custommonkey.xmlunit.XMLUnit;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class MediaRestServiceImplTest {

    public static final MediaType JSON;
    public static final MediaType XML;

    static {
        Map<String, String> params = new HashMap<>();
        JSON = new MediaType("application", "json", params);
        XML = new MediaType("application", "xml", params);
    }

    private static final ObjectMapper mapper = new ObjectMapper();


    Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();

    @Before
    public void setup() {
        MediaRestService testObject = new MediaRestServiceImpl();
        dispatcher.getRegistry().addSingletonResource(testObject);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);

        //mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }


    @Test
    public void testList() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/media?mock=true");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<PagedResult<MediaObject>> typeRef = new TypeReference<PagedResult<MediaObject>>() {};

        PagedResult<MediaObject> result = mapper.readValue(response.getContentAsString(), typeRef);

        assertEquals(Integer.valueOf(50), result.getSize());

        MediaObject object = result.getList().get(0);
    }

    @Test
    public void testSearch() throws Exception {
        MockHttpRequest request = MockHttpRequest.post("/media?mock=true");
        request.contentType(MediaType.APPLICATION_JSON);

        MediaForm form = new MediaForm();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.writeValue(out, form);
        request.content(out.toByteArray());

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));


        TypeReference<PagedResult<MediaObject>> typeRef = new TypeReference<PagedResult<MediaObject>>() {
        };

        PagedResult<MediaObject> result = mapper.readValue(response.getContentAsString(), typeRef);
        assertEquals(Integer.valueOf(50), result.getSize());
    }

    @Test
    public void testGet() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/media/123?mock=true");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));
        System.out.println(response.getContentAsString());
        Program program = mapper.readValue(response.getContentAsString(), Program.class); // FAILS!
        Program compareTo = MediaTestDataBuilder.program().constrained().build();

        assertEquals(compareTo.getMainTitle(), program.getMainTitle());


    }


    @Test
    public void testGetXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/media/123?mock=true");
        request.accept(MediaType.APPLICATION_XML);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));
        System.out.println(response.getContentAsString());
        Program program = JAXB.unmarshal(new StringReader(response.getContentAsString()), Program.class);
        Program compareTo = MediaTestDataBuilder.program().constrained().build();

        assertEquals(compareTo.getMainTitle(), program.getMainTitle());


    }

    @Test
    public void testNotFound() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/media/BESTAATNIET");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        assertEquals(response.getErrorMessage(), 200, response.getStatus()); // FAILS
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));
        System.out.println(response.getContentAsString());

        Program program = mapper.readValue(response.getContentAsString(), Program.class);
        assertEquals(MediaBuilder.program().build(), program);


    }


    @Test
    public void testGetMembers() throws URISyntaxException, IOException {
        MockHttpRequest request = MockHttpRequest.post("/media/123/members?mock=true");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<PagedResult<MediaObject>> typeRef = new TypeReference<PagedResult<MediaObject>>() {
        };

        PagedResult<MediaObject> result = mapper.readValue(response.getContentAsString(), typeRef);
        assertEquals(Integer.valueOf(50), result.getSize());

    }

    @Test
    public void testGetEpisodies() {
        // TODO
    }

    @Test
    public void testgetSegments() {
        // TODO
    }
}
