package nl.vpro.api.rs.v2.page;

import nl.vpro.domain.api.pages.Page;
import org.codehaus.jackson.map.ObjectMapper;
import org.custommonkey.xmlunit.XMLUnit;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class PageRestServiceImplTest {
    public static final MediaType JSON;

    static {
        Map<String, String> params = new HashMap<>();
        JSON = new MediaType("application", "json", params);
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();

    @Before
    public void setUp() throws Exception {
        PageRestService testObject = new PageRestServiceImpl();
        dispatcher.getRegistry().addSingletonResource(testObject);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);

    }

    @Test
    public void testList() throws Exception {

    }

    @Test
    public void testSearch() throws Exception {

    }

    @Test
    public void testGet() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/pages/123?mock=true");
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));
        Page page = mapper.readValue(response.getContentAsString(), Page.class);
        assertEquals("{\"id\":\"4b748d32-8006-4f0a-8aac-6d8d5c89a847\",\"title\":\"Groot brein in klein dier\",\"body\":null,\"summary\":\"Een klein, harig beestje met het gewicht van een paperclip was mogelijk de directe voorouder van alle hedendaagse zoogdieren, waaronder de mens. Levend in de schaduw van de dinosaurussen kroop het diertje 195 miljoen jaar geleden tussen de planten door, op zoek naar insecten die het met zijn vlijmscherpe tandjes vermaalde. Het is de oudste zoogdierachtige die tot nu toe is gevonden.\",\"deepLink\":\"http://www.wetenschap24.nl/groot-brein-in-klein-dier.html\",\"pageType\":\"Artikel\",\"brand\":{\"site\":\"http://www.wetenschap24.nl\",\"title\":\"Wetenschap 24\"},\"author\":\"superuser\",\"mainImage\":{\"url\":\"http://www.wetenschap24.nl/eenkleinharigbeest.jpg\"}}", response.getContentAsString());

        assertEquals("4b748d32-8006-4f0a-8aac-6d8d5c89a847", page.getId());

    }

}
