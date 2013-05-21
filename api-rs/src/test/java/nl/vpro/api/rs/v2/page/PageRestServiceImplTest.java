package nl.vpro.api.rs.v2.page;

import nl.vpro.api.rs.v2.AbstractServiceImplTest;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.page.Page;
import org.codehaus.jackson.type.TypeReference;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class PageRestServiceImplTest extends AbstractServiceImplTest {

    @Override
    protected Object getTestObject() {
        return new PageRestServiceImpl();
    }

    @Test
    public void testList() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/pages?mock=true");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<Result<Page>> typeRef = new TypeReference<Result<Page>>() {
        };
        Result<Page> pages = mapper.readValue(response.getContentAsString(), typeRef);

        assertEquals(Integer.valueOf(50), pages.getSize());
        assertEquals(Integer.valueOf(0), pages.getOffset());
        assertEquals(Integer.valueOf(100), pages.getTotal());
        assertEquals("Groot brein in klein dier", pages.getList().get(0).getTitle());
        assertEquals("urn:vpro:media:program:1234", pages.getList().get(0).getMediaIds().get(0));


    }

    @Test
    public void testListXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/pages?mock=true");
        request.accept(MediaType.APPLICATION_XML);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));

        Result<Page> pages = JAXB.unmarshal(new StringReader(response.getContentAsString()), Result.class);

        assertEquals(Integer.valueOf(50), pages.getSize());
        assertEquals(Integer.valueOf(0), pages.getOffset());
        assertEquals(Integer.valueOf(100), pages.getTotal());
        assertEquals("Groot brein in klein dier", pages.getList().get(0).getTitle());


    }

    @Test
    public void testSearch() throws Exception {
        MockHttpRequest request = MockHttpRequest.post("/pages?mock=true");
        request.contentType(MediaType.APPLICATION_JSON);
        MockHttpResponse response = new MockHttpResponse();
        PageForm form = new PageForm();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.writeValue(out, form);
        request.content(out.toByteArray());
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        TypeReference<Result<Page>> typeRef = new TypeReference<Result<Page>>() {
        };
        Result<Page> pages = mapper.readValue(response.getContentAsString(), typeRef);

        assertEquals(Integer.valueOf(50), pages.getSize());
        assertEquals(Integer.valueOf(0), pages.getOffset());
        assertEquals(Integer.valueOf(100), pages.getTotal());
        assertEquals("Groot brein in klein dier", pages.getList().get(0).getTitle());

    }

    @Test
    public void testSearchXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.post("/pages?mock=true");
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
        Result<Page> pages = JAXB.unmarshal(new StringReader(response.getContentAsString()), Result.class);

        assertEquals(Integer.valueOf(50), pages.getSize());
        assertEquals(Integer.valueOf(0), pages.getOffset());
        assertEquals(Integer.valueOf(100), pages.getTotal());
        assertEquals("Groot brein in klein dier", pages.getList().get(0).getTitle());


    }

    @Test
    public void testGet() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/pages/123?mock=true");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        Page page = mapper.readValue(response.getContentAsString(), Page.class);

        assertEquals("{\"title\":\"Groot brein in klein dier\",\"body\":\"bla bla bla bla\",\"summary\":\"Een klein, harig beestje met het gewicht van een paperclip was mogelijk de directe voorouder van alle hedendaagse zoogdieren, waaronder de mens. Levend in de schaduw van de dinosaurussen kroop het diertje 195 miljoen jaar geleden tussen de planten door, op zoek naar insecten die het met zijn vlijmscherpe tandjes vermaalde. Het is de oudste zoogdierachtige die tot nu toe is gevonden.\",\"deepLink\":\"http://www.wetenschap24.nl/nieuws/artikelen/2001/mei/Groot-brein-in-klein-dier.html\",\"pageType\":\"Artikel\",\"brand\":{\"site\":\"http://www.wetenschap24.nl\",\"title\":\"Wetenschap 24\"},\"author\":\"superuser\",\"mainImage\":{\"url\":\"http://www.wetenschap24.nl/.imaging/stk/wetenschap/vtk-imagegallery-normal/media/wetenschap/noorderlicht/artikelen/2001/May/3663525/original/3663525.jpeg\"},\"mediaIds\":[\"urn:vpro:media:program:1234\",\"urn:vpro:media:group:4321\"],\"id\":\"4b748d32-8006-4f0a-8aac-6d8d5c89a847\"}", response.getContentAsString());

        assertEquals("4b748d32-8006-4f0a-8aac-6d8d5c89a847", page.getId());
        assertEquals("http://www.wetenschap24.nl/.imaging/stk/wetenschap/vtk-imagegallery-normal/media/wetenschap/noorderlicht/artikelen/2001/May/3663525/original/3663525.jpeg",
                page.getMainImage().getUrl().toString());

    }


    @Test
    public void testGetXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/pages/123?mock=true");
        request.accept(MediaType.APPLICATION_XML);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));

        Page page = JAXB.unmarshal(new StringReader(response.getContentAsString()), Page.class);
        assertEquals("4b748d32-8006-4f0a-8aac-6d8d5c89a847", page.getId());

        System.out.println(response.getContentAsString());
        String expected ="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<pages:page xmlns:pages=\"urn:vpro:pages:2013\" id=\"4b748d32-8006-4f0a-8aac-6d8d5c89a847\">\n" +
                "  <pages:title>Groot brein in klein dier</pages:title>\n" +
                "  <pages:body>bla bla bla bla</pages:body>\n" +
                "  <pages:summary>Een klein, harig beestje met het gewicht van een paperclip was mogelijk de directe voorouder van alle hedendaagse zoogdieren, waaronder de mens. Levend in de schaduw van de dinosaurussen kroop het diertje 195 miljoen jaar geleden tussen de planten door, op zoek naar insecten die het met zijn vlijmscherpe tandjes vermaalde. Het is de oudste zoogdierachtige die tot nu toe is gevonden.</pages:summary>\n" +
                "  <pages:deepLink>http://www.wetenschap24.nl/nieuws/artikelen/2001/mei/Groot-brein-in-klein-dier.html</pages:deepLink>\n" +
                "  <pages:pageType>Artikel</pages:pageType>\n" +
                "  <pages:brand>\n" +
                "    <pages:site>http://www.wetenschap24.nl</pages:site>\n" +
                "    <pages:title>Wetenschap 24</pages:title>\n" +
                "  </pages:brand>\n" +
                "  <pages:author>superuser</pages:author>\n" +
                "  <pages:mainImage>\n" +
                "    <pages:url>http://www.wetenschap24.nl/.imaging/stk/wetenschap/vtk-imagegallery-normal/media/wetenschap/noorderlicht/artikelen/2001/May/3663525/original/3663525.jpeg</pages:url>\n" +
                "  </pages:mainImage>\n" +
                "  <pages:mediaIds>\n" +
                "    <pages:id>urn:vpro:media:program:1234</pages:id>\n" +
                "    <pages:id>urn:vpro:media:group:4321</pages:id>\n" +
                "  </pages:mediaIds>\n" +
                "</pages:page>\n";

        Diff diff = XMLUnit.compareXML(expected, response.getContentAsString());
        assertTrue(diff.toString() + " " + response.getContentAsString(), diff.similar());

    }
}
