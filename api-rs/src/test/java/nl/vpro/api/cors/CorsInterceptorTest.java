package nl.vpro.api.cors;

import java.util.ArrayList;
import java.util.Collections;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.core.ResteasyContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    public void resetMocks() {
        reset(corsPolicy);
    }

    @Test
    public void testFilterDisabled() {

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
    public void testIEHack() {

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();


        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request);

        assertThat(request.getHeaders().get(HttpHeaders.CONTENT_TYPE)).containsExactly("application/json");
    }

    @Test
    public void testIEHack2() {

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
