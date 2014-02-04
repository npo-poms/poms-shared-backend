package nl.vpro.api.rs.v2.media;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import nl.vpro.api.rs.v2.AbstractRestServiceImplTest;
import nl.vpro.domain.api.*;
import nl.vpro.domain.api.Error;
import nl.vpro.domain.api.SearchResult;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.MediaSearch;
import nl.vpro.domain.api.media.MediaService;
import nl.vpro.domain.api.transfer.*;
import nl.vpro.domain.media.Group;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.Program;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**           ´
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Ignore("fix these when we no longer return response objects!!!") // TODO
public class MediaRestServiceImplTest extends AbstractRestServiceImplTest {

    MediaService mediaService = mock(MediaService.class);

    @Before
    public void resetMocks() {
        reset(mediaService);
    }

    @Override
    protected Object getTestObject() {
        return new MediaRestServiceImpl(mediaService);
    }


    @Test
    public void testList() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/media");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        verify(mediaService).list(0l, 10);
    }

    @Test
    public void testListException() throws Exception {
        when(mediaService.list(eq(0l), anyInt()))
                .thenThrow(new RuntimeException("Er is wat misgegaan"));


        MockHttpRequest request = MockHttpRequest.get("/media");
        request.accept(MediaType.APPLICATION_XML);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 500, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));

        nl.vpro.domain.api.Error result = JAXB.unmarshal(new StringReader(response.getContentAsString()), nl.vpro.domain.api.Error.class);

        assertThat(result.getMessage()).isEqualTo("java.lang.RuntimeException: Er is wat misgegaan");
        assertThat(result.getStatus()).isEqualTo(500);
    }

    @Test
    public void testListExceptionJson() throws Exception {
        when(mediaService.list(eq(0l), anyInt())).thenThrow(new RuntimeException("Er is wat misgegaan"));

        MockHttpRequest request = MockHttpRequest.get("/media");
        request.accept(MediaType.APPLICATION_JSON);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 500, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        nl.vpro.domain.api.Error result = mapper.readValue(response.getContentAsString(), nl.vpro.domain.api.Error.class);

        assertThat(result.getMessage()).isEqualTo("java.lang.RuntimeException: Er is wat misgegaan");
        assertThat(result.getStatus()).isEqualTo(500);
    }

    @Test
    public void testListMock() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/media?mock=true");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<nl.vpro.domain.api.transfer.Result<MediaObject>> typeRef = new TypeReference<nl.vpro.domain.api.transfer.Result<MediaObject>>() {
        };

        nl.vpro.domain.api.transfer.Result<MediaObject> result = mapper.readValue(response.getContentAsString(), typeRef);

        assertEquals(Integer.valueOf(10), result.getSize());

        MediaObject object = result.getList().get(0);
        assertEquals("Main title", object.getMainTitle());
    }


    @Test
    public void testListXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/media?mock=true");
        request.accept(MediaType.APPLICATION_XML);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage() + " " + response.getContentAsString(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));

        nl.vpro.domain.api.transfer.Result<MediaObject> result = JAXB.unmarshal(new StringReader(response.getContentAsString()), nl.vpro.domain.api.transfer.Result.class);

        assertEquals(Integer.valueOf(10), result.getSize());

        MediaObject object = result.getList().get(0);
        assertEquals("Main title", object.getMainTitle());

    }


    @Test
    public void testSearch() throws Exception {
        MockHttpRequest request = MockHttpRequest.post("/media?mock=true");
        request.contentType(MediaType.APPLICATION_JSON);
        request.accept(MediaType.APPLICATION_JSON);

        MediaForm form = new MediaForm();
        MediaSearch mediaSearch = new MediaSearch();
        mediaSearch.setBroadcasters(Arrays.asList(new TextMatcher("VPRO")));
        form.setSearches(mediaSearch);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.out.println(new String(out.toByteArray()));
        mapper.writeValue(out, form);
        request.content(out.toByteArray());

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        System.out.println(new String(out.toByteArray()));

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

        nl.vpro.domain.api.Error error = mapper.readValue(response.getContentAsString(), nl.vpro.domain.api.Error.class);
        assertEquals(Integer.valueOf(400), error.getStatus());


    }


    @Test
    public void testGetMembers() throws URISyntaxException, IOException {
        when(mediaService.findMembers(any(String.class), isNull(String.class), isNull(MediaForm.class), eq(0l), anyInt()))
            .thenReturn(new MediaSearchResult(new ArrayList<SearchResultItem<? extends MediaObject>>(), 0l, 0, 10l));

        when(mediaService.load("123")).thenReturn(new Program());

        MockHttpRequest request = MockHttpRequest.get("/media/123/members");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<MediaResult> typeRef = new TypeReference<MediaResult>() {
        };

        MediaResult result = mapper.readValue(response.getContentAsString(), typeRef);
        assertThat(result.getTotal()).isEqualTo(10);
    }


    @Test
    public void testGetMembersNotFound() throws URISyntaxException, IOException {
        when(mediaService.findMembers(any(String.class), isNull(String.class), isNull(MediaForm.class), eq(0l), anyInt()))
            .thenReturn(new MediaSearchResult(new ArrayList<SearchResultItem<? extends MediaObject>>(), 0l, 0, 10l));

        when(mediaService.load("123")).thenReturn(null);

        MockHttpRequest request = MockHttpRequest.get("/media/123/members");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 400, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<Error> typeRef = new TypeReference<Error>() {
        };

        Error result = mapper.readValue(response.getContentAsString(), typeRef);
        assertThat(result.getMessage()).isEqualTo("No media for id 123");
    }

    @Test
    public void testGetEpisodes() throws URISyntaxException, IOException {

        when(mediaService.findEpisodes(any(String.class), isNull(String.class), isNull(MediaForm.class), eq(0l), anyInt()))
            .thenReturn(new ProgramSearchResult(new ArrayList<SearchResultItem<? extends Program>>(), 0l, 0, 10l));

        when(mediaService.load("123")).thenReturn(new Group());

        MockHttpRequest request = MockHttpRequest.get("/media/123/episodes");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<nl.vpro.domain.api.transfer.Result<Program>> typeRef = new TypeReference<nl.vpro.domain.api.transfer.Result<Program>>() {
        };

        nl.vpro.domain.api.transfer.Result<Program> result = mapper.readValue(response.getContentAsString(), typeRef);
        assertEquals(Long.valueOf(10), result.getTotal());
    }

    @Test
    public void testGetEpisodesNotFound() throws URISyntaxException, IOException {

        when(mediaService.findEpisodes(any(String.class), isNull(String.class), isNull(MediaForm.class), eq(0l), anyInt()))
            .thenReturn(new ProgramSearchResult(new ArrayList<SearchResultItem<? extends Program>>(), 0l, 0, 10l));

        when(mediaService.load("123")).thenReturn(null);

        MockHttpRequest request = MockHttpRequest.get("/media/123/episodes");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 400, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<Error> typeRef = new TypeReference<Error>() {
        };

        Error result = mapper.readValue(response.getContentAsString(), typeRef);
        assertThat(result.getMessage()).isEqualTo("No media for id 123");
    }

    @Test
    public void testGetDescendants() throws URISyntaxException, IOException {
        when(mediaService.findDescendants(any(String.class), isNull(String.class), isNull(MediaForm.class), eq(0l), anyInt()))
            .thenReturn(new MediaSearchResult(new ArrayList<SearchResultItem<? extends MediaObject>>(), 0l, 0, 10l));

        when(mediaService.load("123")).thenReturn(new Group());

        MockHttpRequest request = MockHttpRequest.get("/media/123/descendants");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<nl.vpro.domain.api.transfer.Result<MediaObject>> typeRef = new TypeReference<nl.vpro.domain.api.transfer.Result<MediaObject>>() {
        };

        nl.vpro.domain.api.transfer.Result<Program> result = mapper.readValue(response.getContentAsString(), typeRef);
        assertEquals(Long.valueOf(10), result.getTotal());
    }


    @Test
    public void testGetDescendantsNotFound() throws URISyntaxException, IOException {
        when(mediaService.findDescendants(any(String.class), isNull(String.class), isNull(MediaForm.class), eq(0l), anyInt()))
            .thenReturn(new MediaSearchResult(new ArrayList<SearchResultItem<? extends MediaObject>>(), 0l, 0, 10l));

        when(mediaService.load("123")).thenReturn(null);


        MockHttpRequest request = MockHttpRequest.get("/media/123/descendants");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 400, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<Error> typeRef = new TypeReference<Error>() {
        };

        Error result = mapper.readValue(response.getContentAsString(), typeRef);
        assertThat(result.getMessage()).isEqualTo("No media for id 123");
    }

}
