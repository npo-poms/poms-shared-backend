package nl.vpro.api.rs.v3.profile;

import java.io.StringReader;
import java.util.Date;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import nl.vpro.api.rs.v3.AbstractRestServiceImplTest;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.ProfileService;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class ProfileRestServiceImplTest extends AbstractRestServiceImplTest {

    private final ProfileService profileService = Mockito.mock(ProfileService.class);

    @Override
    protected Object getTestObject() {
        return new ProfileRestServiceImpl(profileService);
    }

    @Before
    public void setUp() {
        Mockito.reset(profileService);
        when(profileService.getProfile((not(eq("geschiedenis"))))).thenReturn(null);
        when(profileService.getProfile("geschiedenis")).thenReturn(new Profile("geschiedenis"));
        when(profileService.getProfile(eq("geschiedenis"), any(Date.class))).thenReturn(new Profile("geschiedenis"));
    }

    @Test
    public void testGetProfileWhenFound() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/profiles/geschiedenis");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void testGetProfileWhenNotFound() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/profiles/notfound");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    public void testGetProfileOn() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/profiles/geschiedenis?time=100");

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);

        verify(profileService).getProfile(eq("geschiedenis"), eq(new Date(100l)));
    }

    @Ignore("Temporary enabled XML output for use in Swagger API docs")
    @Test
    public void testLoadJson() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/profiles/geschiedenis");
        request.accept(MediaType.APPLICATION_JSON);

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertEquals(response.getErrorMessage(), 406, response.getStatus());
    }

    @Test
    public void testLoadXml() throws Exception {
        MockHttpRequest request = MockHttpRequest.get("/profiles/geschiedenis");
        request.accept(MediaType.APPLICATION_XML);

        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertEquals(XML, response.getOutputHeaders().get("Content-Type").get(0));

        Profile profile = JAXB.unmarshal(new StringReader(response.getContentAsString()), Profile.class);
        assertEquals("geschiedenis", profile.getName());
    }
}
