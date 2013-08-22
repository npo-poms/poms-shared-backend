package nl.vpro.api.cors;

import java.util.ArrayList;
import java.util.Arrays;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class CorsInterceptorTest {
    CorsPolicy corsPolicy = mock(CorsPolicy.class);

    @Before
    public void resetMocks() {
        reset(corsPolicy);
    }

    @Test
    public void testFilterDisabled() throws Exception {

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        when(corsPolicy.isEnabled()).thenReturn(Boolean.FALSE);
        when(corsPolicy.allowedOriginAndMethod(anyString(), anyString())).thenReturn(Boolean.TRUE);


        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(request.getHeaderString(CorsHeaders.ORIGIN)).thenReturn("localhost");
        when(request.getMethod()).thenReturn("GET");
        when(response.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request, response);

        assertThat(headers.get("Access-Control-Allow-Origin")).containsExactly("localhost");

    }

    @Test
    public void testIEHack() throws Exception {

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();


        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request);

        assertThat(request.getHeaders().get("Content-Type")).isEqualTo(Arrays.asList("application/json"));
    }

    @Test
    public void testIEHack2() throws Exception {

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put("Content-Type", new ArrayList<>(Arrays.asList("text/plain")));


        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request);

        assertThat(request.getHeaders().get("Content-Type")).isEqualTo(Arrays.asList("application/json"));
    }
}
