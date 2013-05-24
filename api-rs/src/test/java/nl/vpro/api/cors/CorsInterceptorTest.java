package nl.vpro.api.cors;

import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class CorsInterceptorTest {
    CorsPolicy corsPolicy = mock(CorsPolicy.class);

    @Test
    public void testFilter() throws Exception {

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        when(corsPolicy.isEnabled()).thenReturn(Boolean.TRUE);
        when(corsPolicy.allowedOriginAndMethod(anyString(), anyString())).thenReturn(Boolean.TRUE);


        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response  = mock(ContainerResponseContext.class);
        when(request.getHeaderString(CorsHeaders.ORIGIN)).thenReturn("localhost");
        when(request.getMethod()).thenReturn("GET");
        when(response.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request, response);

        assertThat(headers.get("Access-Control-Allow-Headers")).containsExactly("x-http-method-override, origin, content-type, accept");
        assertThat(headers.get("Access-Control-Allow-Origin")).containsExactly("localhost");
        assertThat(headers.get("Access-Control-Allow-Methods")).containsExactly("GET, HEAD, OPTIONS, POST");


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

        assertThat(headers).isEmpty();
    }

    @Test
    public void testFilterNotAllowed() throws Exception {

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        when(corsPolicy.isEnabled()).thenReturn(Boolean.TRUE);
        when(corsPolicy.allowedOriginAndMethod(anyString(), anyString())).thenReturn(Boolean.FALSE);


        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(request.getHeaderString(CorsHeaders.ORIGIN)).thenReturn("localhost");
        when(request.getMethod()).thenReturn("GET");
        when(response.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request, response);

        assertThat(headers).isEmpty();
    }
}
