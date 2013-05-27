package nl.vpro.api.rs.v2.profile;

import nl.vpro.api.profile.ProfileService;
import nl.vpro.api.profile.ProfileServiceImpl;
import nl.vpro.api.rs.v2.AbstractRestServiceImplTest;
import nl.vpro.domain.api.profile.Profile;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

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
    public void testLoad() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/profiles/geschiedenis");
        request.accept(MediaType.APPLICATION_JSON);
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(JSON, response.getOutputHeaders().get("Content-Type").get(0));

        Profile page = mapper.readValue(response.getContentAsString(), Profile.class);

    }
}
