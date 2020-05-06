package nl.vpro.api.rs.interceptors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import nl.vpro.domain.api.media.Redirector;
import nl.vpro.poms.shared.Headers;


/**
 * @author Michiel Meeuwissen
 * @since 5.13
 */
@Provider
public class NPOHeadersInterceptor implements ContainerResponseFilter, ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        Map<String, String> redirects = Redirector.REDIRECTS.get();
        if (redirects != null) {
            response.getHeaders().putSingle(Headers.NPO_REDIRECTS, redirects);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            response.getHeaders().putSingle(Headers.NPO_CURRENT_USER, authentication.getName());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Redirector.REDIRECTS.set(new HashMap<>());
    }
}
