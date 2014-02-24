package nl.vpro.api.rs.v2.page;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import nl.vpro.api.rs.v2.AbstractRestServiceImplTest;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.SearchResult;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.api.page.PageService;
import nl.vpro.domain.page.Page;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Ignore("Fis this when we start working on the page API it's outdated")
public class PageNodeRestServiceImplTest extends AbstractRestServiceImplTest {

    final PageService pageService = mock(PageService.class);


    @Override
    protected Object getTestObject() {
        return new PageNodeRestServiceImpl(pageService);
    }

    @Test
    public void testList() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/pages?mock=true&max=50");
        //request.accept(MediaType.APPLICATION_JSON);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<Result<Page>> typeRef = new TypeReference<Result<Page>>() {
        };
        Result<Page> pages = mapper.readValue(response.getContentAsString(), typeRef);

        assertEquals(Integer.valueOf(50), pages.getSize());
        assertEquals(Long.valueOf(0), pages.getOffset());
        assertEquals(Long.valueOf(100), pages.getTotal());
        assertEquals("Groot brein in klein dier", pages.getList().get(0).getTitle());


    }

    @Test
    public void testListXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/pages?mock=true&max=50");
        request.accept(MediaType.APPLICATION_XML);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage() + " " + response.getContentAsString(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));

        Result<Page> pages = JAXB.unmarshal(new StringReader(response.getContentAsString()), Result.class);

        assertEquals(Integer.valueOf(50), pages.getSize());
        assertEquals(Long.valueOf(0), pages.getOffset());
        assertEquals(Long.valueOf(100), pages.getTotal());
        assertEquals("Groot brein in klein dier", pages.getList().get(0).getTitle());


    }

    @Test
    public void testSearch() throws Exception {
        MockHttpRequest request = MockHttpRequest.post("/pages?mock=true&max=50");
        request.contentType(MediaType.APPLICATION_JSON);
        MockHttpResponse response = new MockHttpResponse();

        String pageForm = "{\"highlight\":false,\"searches\":{\"publishers\":[{\"value\":\"kro\"}]}}";
        request.content(pageForm.getBytes());


        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<SearchResult<Page>> typeRef = new TypeReference<SearchResult<Page>>() {
        };
        SearchResult<Page> pages = mapper.readValue(response.getContentAsString(), typeRef);

        assertEquals(Integer.valueOf(50), pages.getSize());
        assertEquals(Long.valueOf(0), pages.getOffset());
        assertEquals(Long.valueOf(100), pages.getTotal());
        assertEquals("Groot brein in klein dier", pages.getList().get(0).getResult().getTitle());

    }

    @Test
    public void testSearchXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.post("/pages?mock=true&max=50");
        request.contentType(MediaType.APPLICATION_XML);
        request.accept(MediaType.APPLICATION_XML);

        PageForm form = new PageForm();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JAXB.marshal(form, out);

        request.content(out.toByteArray());
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));
        SearchResult<Page> pages = JAXB.unmarshal(new StringReader(response.getContentAsString()), SearchResult.class);

        assertEquals(Integer.valueOf(50), pages.getSize());
        assertEquals(Long.valueOf(0), pages.getOffset());
        assertEquals(Long.valueOf(100), pages.getTotal());
        assertEquals("Groot brein in klein dier", pages.getList().get(0).getResult().getTitle());


    }
/* get calls were dropped
    @Test
    public void testGet() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/pages/123?mock=true");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        Page page = mapper.readValue(response.getContentAsString(), Page.class);

        assertEquals("{\"objectType\":\"article\",\"title\":\"Groot brein in klein dier\",\"subtitle\":\"De naakte molrat heeft ‘m\",\"author\":\"superuser\",\"bodies\":[\"bla bla bla bla\"],\"summary\":\"Een klein, harig beestje met het gewicht van een paperclip was mogelijk de directe voorouder van alle hedendaagse zoogdieren, waaronder de mens. Levend in de schaduw van de dinosaurussen kroop het diertje 195 miljoen jaar geleden tussen de planten door, op zoek naar insecten die het met zijn vlijmscherpe tandjes vermaalde. Het is de oudste zoogdierachtige die tot nu toe is gevonden.\",\"url\":\"http://www.wetenschap24.nl/nieuws/artikelen/2001/mei/Groot-brein-in-klein-dier.html\",\"portal\":{\"url\":\"http://www.wetenschap24.nl\",\"name\":\"Wetenschap 24\"},\"images\":[{\"imageUrl\":\"http://www.wetenschap24.nl/.imaging/stk/wetenschap/vtk-imagegallery-normal/media/wetenschap/noorderlicht/artikelen/2001/May/3663525/original/3663525.jpeg\"}],\"keywords\":[\"keyword1\",\"keyword2\",\"keyword3\"],\"tags\":[\"tag1\",\"tag2\",\"tag3\"],\"broadcasters\":[\"VPRO\",\"KRO\"],\"pid\":\"4b748d32-8006-4f0a-8aac-6d8d5c89a847\",\"sortDate\":1370424584330}", response.getContentAsString());

        assertEquals("4b748d32-8006-4f0a-8aac-6d8d5c89a847", page.getPid());
        assertEquals("http://www.wetenschap24.nl/.imaging/stk/wetenschap/vtk-imagegallery-normal/media/wetenschap/noorderlicht/artikelen/2001/May/3663525/original/3663525.jpeg",
                page.getImages().get(0).getImageUrl());

    }


    @Test
    public void testGetXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/pages/123?mock=true");
        request.accept(MediaType.APPLICATION_XML);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));

        Page page = (Page) JAXBContext.newInstance("nl.vpro.domain.page").createUnmarshaller().unmarshal(new StringReader(response.getContentAsString()));
        assertEquals("4b748d32-8006-4f0a-8aac-6d8d5c89a847", page.getPid());

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<pages:article xmlns:shared=\"urn:vpro:shared:2009\" xmlns:pages=\"urn:vpro:pages:2013\" xmlns=\"urn:vpro:media:2009\" pid=\"4b748d32-8006-4f0a-8aac-6d8d5c89a847\" sortDate=\"2013-06-05T11:29:44.330+02:00\">\n" +
            "  <pages:title>Groot brein in klein dier</pages:title>\n" +
            "  <pages:subtitle>De naakte molrat heeft ‘m</pages:subtitle>\n" +
            "  <pages:author>superuser</pages:author>\n" +
            "  <pages:body>bla bla bla bla</pages:body>\n" +
            "  <pages:summary>Een klein, harig beestje met het gewicht van een paperclip was mogelijk de directe voorouder van alle hedendaagse zoogdieren, waaronder de mens. Levend in de schaduw van de dinosaurussen kroop het diertje 195 miljoen jaar geleden tussen de planten door, op zoek naar insecten die het met zijn vlijmscherpe tandjes vermaalde. Het is de oudste zoogdierachtige die tot nu toe is gevonden.</pages:summary>\n" +
            "  <pages:url>http://www.wetenschap24.nl/nieuws/artikelen/2001/mei/Groot-brein-in-klein-dier.html</pages:url>\n" +
            "  <pages:portal url=\"http://www.wetenschap24.nl\">Wetenschap 24</pages:portal>\n" +
            "  <pages:image>\n" +
            "    <pages:imageUrl>http://www.wetenschap24.nl/.imaging/stk/wetenschap/vtk-imagegallery-normal/media/wetenschap/noorderlicht/artikelen/2001/May/3663525/original/3663525.jpeg</pages:imageUrl>\n" +
            "  </pages:image>\n" +
            "  <pages:keyword>keyword1</pages:keyword>\n" +
            "  <pages:keyword>keyword2</pages:keyword>\n" +
            "  <pages:keyword>keyword3</pages:keyword>\n" +
            "  <pages:tag>tag1</pages:tag>\n" +
            "  <pages:tag>tag2</pages:tag>\n" +
            "  <pages:tag>tag3</pages:tag>\n" +
            "  <pages:broadcaster id=\"VPRO\">VPRO</pages:broadcaster>\n" +
            "  <pages:broadcaster id=\"KRO\">KRO</pages:broadcaster>\n" +
            "</pages:article>\n";

//        Assertions.assertThat(response.getContentAsString()).isEqualTo(expected);

        Diff diff = XMLUnit.compareXML(expected, response.getContentAsString());
        assertTrue(diff.toString() + " " + response.getContentAsString(), diff.similar());
    }*/
}
