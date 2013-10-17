package nl.vpro.api.rs.v2.profile;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;

import org.codehaus.jackson.type.TypeReference;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.api.profile.ProfileServiceImpl;
import nl.vpro.api.rs.v2.AbstractRestServiceImplTest;
import nl.vpro.domain.api.profile.Profile;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@RunWith(value = Parameterized.class)
public class ProfileRestServiceImplTest extends AbstractRestServiceImplTest {

    private boolean mock;

    private ProfileService profileService = Mockito.mock(ProfileService.class);

    public ProfileRestServiceImplTest(boolean mock) {
        this.mock = mock;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{{Boolean.FALSE}, {Boolean.TRUE}};
        return Arrays.asList(data);
    }

    @Override
    protected Object getTestObject() {
        return new ProfileRestServiceImpl(profileService);
    }

    @Before
    public void setUp() {
        Mockito.reset(profileService);
        when(profileService.getProfile("geschiedenis")).thenReturn(new Profile("geschiedenis"));
        when(profileService.getProfiles()).thenReturn(new TreeSet<>(Arrays.asList(new Profile("geschiedenis"))));
    }

    @Test
    public void testLoadJson() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/profiles/geschiedenis?mock=" + mock);
        request.accept(MediaType.APPLICATION_JSON);

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 406, response.getStatus());
    }

    @Test
    public void testLoadXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/profiles/geschiedenis?mock=" + mock);
        request.accept(MediaType.APPLICATION_XML);

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 200, response.getStatus());
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));

        Profile profile = JAXB.unmarshal(new StringReader(response.getContentAsString()), Profile.class);
        assertEquals("geschiedenis", profile.getName());
    }
}
