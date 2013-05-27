package nl.vpro.api.rs.v2.profile;

import nl.vpro.api.profile.ProfileService;
import nl.vpro.api.profile.ProfileServiceImpl;
import nl.vpro.api.rs.v2.AbstractRestServiceImplTest;
import nl.vpro.domain.api.profile.Profile;
import org.codehaus.jackson.type.TypeReference;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class ProfileRestServiceImplTest extends AbstractRestServiceImplTest {

    static ProfileService profileService;
    @BeforeClass
    public static void setupProfileService() throws JAXBException, IOException, SAXException {
        // not really needed to mock, we have a real profile service
        profileService = new ProfileServiceImpl("geschiedenis");
    }

    @Override
    protected Object getTestObject() {
        return new ProfileRestServiceImpl(profileService);
    }
    @Test
    public void testLoadJson() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/profiles/geschiedenis");
        request.accept(MediaType.APPLICATION_JSON);

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));


        TypeReference<Profile> typeRef = new TypeReference<Profile>() {
        };

        // TODO doesnt work
        ///Profile profile = mapper.readValue(response.getContentAsString(), typeRef);

    }

    @Test
    public void testLoadXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/profiles/geschiedenis");
        request.accept(MediaType.APPLICATION_XML);

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));

        Profile profile = JAXB.unmarshal(new StringReader(response.getContentAsString()), Profile.class);
        assertEquals("geschiedenis", profile.getName());
        assertNotNull(profile.getPageProfile());
        assertNull(profile.getMediaProfile());

    }
}
