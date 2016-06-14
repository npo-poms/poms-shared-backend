package nl.vpro.api.cors;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
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

        ResteasyProviderFactory.pushContext(HttpServletResponse.class, mock(HttpServletResponse.class));
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(request.getHeaderString(HttpHeaders.ORIGIN)).thenReturn("localhost");
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(response.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request, response);

        assertThat(headers.get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).containsExactly("localhost");
    }

    @Test
    public void testIEHack() throws Exception {

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();


        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request);

        assertThat(request.getHeaders().get(HttpHeaders.CONTENT_TYPE)).containsExactly(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    public void testIEHack2() throws Exception {

        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, new ArrayList<>(Collections.singletonList(MediaType.TEXT_PLAIN_VALUE)));


        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getHeaders()).thenReturn(headers);

        CorsInterceptor inst = new CorsInterceptor(corsPolicy);

        inst.filter(request);

        assertThat(request.getHeaders().get(HttpHeaders.CONTENT_TYPE)).containsExactly(MediaType.APPLICATION_JSON_VALUE);
    }
}
