package nl.vpro.api.cors;

import java.util.ArrayList;
import java.util.Collections;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.core.ResteasyContext;
import org.junit.Before;
import org.junit.Test;
import com.google.common.net.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class CorsInterceptorTest {
    private final CorsPolicy corsPolicy = mock(CorsPolicy.class);

    @Before
    public void resetMocks() {
        reset(corsPolicy);
    }

    @Test
    public void testFilterDisabled() throws Exception {

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        when(corsPolicy.isEnabled()).thenReturn(Boolean.FALSE);
        when(corsPolicy.allowedOriginAndMethod(anyString(), anyString())).thenReturn(Boolean.TRUE);

        ResteasyContext.pushContext(HttpServletResponse.class, mock(HttpServletResponse.class));
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(request.getHeaderString(HttpHeaders.ORIGIN)).thenReturn("localhost");
        when(request.getMethod()).thenReturn("GET");
        when(response.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request, response);

        assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).containsExactly("localhost");
    }

    @Test
    public void testIEHack() throws Exception {

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();


        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request);

        assertThat(request.getHeaders().get(HttpHeaders.CONTENT_TYPE)).containsExactly("application/json");
    }

    @Test
    public void testIEHack2() throws Exception {

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, new ArrayList<>(Collections.singletonList("text/plain")));


        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request);

        assertThat(request.getHeaders().get(HttpHeaders.CONTENT_TYPE)).containsExactly("application/json");
    }
}
