package nl.vpro.api.rs.v2.media;

import nl.vpro.api.rs.v2.AbstractServiceImplTest;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.SearchResult;
import nl.vpro.domain.api.SearchResultItem;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.Program;
import org.codehaus.jackson.type.TypeReference;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class MediaRestServiceImplTest extends AbstractServiceImplTest {

    @Override
    protected Object getTestObject() {
        return new MediaRestServiceImpl();
    }

    @Test
    public void testList() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/media?mock=true");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<SearchResult<MediaObject>> typeRef = new TypeReference<SearchResult<MediaObject>>() {
        };
        System.out.println(response.getContentAsString());



        SearchResult<MediaObject> result = mapper.readValue(response.getContentAsString(), typeRef);

        assertEquals(Integer.valueOf(10), result.getSize());

        SearchResultItem<? extends MediaObject> object = result.getList().get(0);
        assertEquals("Main title", object.getResult().getMainTitle());
    }


    @Test
    public void testListXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/media?mock=true");
        request.accept(MediaType.APPLICATION_XML);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage() + " " + response.getContentAsString(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));

        SearchResult<MediaObject> result = JAXB.unmarshal(new StringReader(response.getContentAsString()), SearchResult.class);

        assertEquals(Integer.valueOf(10), result.getSize());

        SearchResultItem<? extends MediaObject> object = result.getList().get(0);
        assertEquals("Main title", object.getResult().getMainTitle());

    }


    @Test
    public void testSearch() throws Exception {
        MockHttpRequest request = MockHttpRequest.post("/media?mock=true");
        request.accept(MediaType.APPLICATION_JSON);

        MediaForm form = new MediaForm();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.writeValue(out, form);
        request.content(out.toByteArray());

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        System.out.println(response.getContentAsString());

        assertEquals(response.getErrorMessage() + " " + response.getContentAsString(), 200, response.getStatus());

        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));


        TypeReference<SearchResult<MediaObject>> typeRef = new TypeReference<SearchResult<MediaObject>>() {
        };

        SearchResult<MediaObject> result = mapper.readValue(response.getContentAsString(), typeRef);
        assertEquals(Integer.valueOf(10), result.getSize());
        assertEquals("foo", result.getList().get(0).getHighlights().get(0).getTerm());

    }


    @Test
    public void testSearchXML() throws Exception {
        MockHttpRequest request = MockHttpRequest.post("/media?mock=true");
        request.contentType(MediaType.APPLICATION_XML);
        request.accept(MediaType.APPLICATION_XML);

        MediaForm form = new MediaForm();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        JAXB.marshal(form, out);
        request.content(out.toByteArray());


        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage() + " " + response.getContentAsString(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));


        SearchResult<MediaObject> result = JAXB.unmarshal(new StringReader(response.getContentAsString()), SearchResult.class);
        assertEquals(Integer.valueOf(10), result.getSize());
        assertEquals("foo", result.getList().get(0).getHighlights().get(0).getTerm());
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
        assertEquals(response.getErrorMessage(), 400, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));
        System.out.println(response.getContentAsString());

        nl.vpro.domain.api.Error error = mapper.readValue(response.getContentAsString(), nl.vpro.domain.api.Error.class);
        assertEquals(Integer.valueOf(400), error.getStatus());


    }


    @Test
    public void testGetMembers() throws URISyntaxException, IOException {
        MockHttpRequest request = MockHttpRequest.get("/media/123/members?mock=true");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<Result<MediaObject>> typeRef = new TypeReference<Result<MediaObject>>() {
        };

        Result<MediaObject> result = mapper.readValue(response.getContentAsString(), typeRef);
        assertEquals(Integer.valueOf(10), result.getSize());

    }

    @Test
    public void testGetEpisodes() throws URISyntaxException, IOException {
        MockHttpRequest request = MockHttpRequest.get("/media/123/episodes?mock=true");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<Result<Program>> typeRef = new TypeReference<Result<Program>>() {
        };

        Result<Program> result = mapper.readValue(response.getContentAsString(), typeRef);
        assertEquals(Integer.valueOf(10), result.getSize());
    }

    @Test
    public void testGetDescendants() throws URISyntaxException, IOException {
        MockHttpRequest request = MockHttpRequest.get("/media/123/descendants?mock=true");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<Result<MediaObject>> typeRef = new TypeReference<Result<MediaObject>>() {
        };

        Result<Program> result = mapper.readValue(response.getContentAsString(), typeRef);
        assertEquals(Integer.valueOf(10), result.getSize());
    }


}
