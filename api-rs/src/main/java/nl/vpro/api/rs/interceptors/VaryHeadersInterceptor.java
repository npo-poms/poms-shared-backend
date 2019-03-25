package nl.vpro.api.rs.interceptors;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import com.google.common.net.HttpHeaders;


/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
@Provider
@Priority(0) // must come last
public class VaryHeadersInterceptor implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {

        response.getHeaders().addAll(HttpHeaders.VARY, HttpHeaders.ACCEPT, HttpHeaders.ACCEPT_LANGUAGE);
        boolean hasCors = response.getHeaderString(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) != null;
        if (hasCors) {
            response.getHeaders().addAll(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        }
    }
}
