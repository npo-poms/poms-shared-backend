package nl.vpro.api.rs.interceptors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import nl.vpro.VersionService;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.media.Redirector;
import nl.vpro.poms.shared.ExtraHeaders;
import nl.vpro.poms.shared.Headers;


/**
 * @author Michiel Meeuwissen
 * @since 5.13
 */
@Provider
public class NPOHeadersInterceptor implements ContainerResponseFilter, ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        try {
            Map<String, String> redirects = Redirector.REDIRECTS.get();
            if (redirects != null && !redirects.isEmpty()) {
                response.getHeaders().putSingle(Headers.NPO_REDIRECTS, redirects);
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                response.getHeaders().putSingle(Headers.NPO_CURRENT_USER, authentication.getName());
            }
            response.getHeaders().putSingle(Headers.NPO_VERSION, VersionService.version());

            ExtraHeaders.get().forEach(p -> {
                response.getHeaders().putSingle(p.getKey(), p.getValue());
            });
            Object entity = response.getEntity();
            if (entity instanceof Result) {
                response.getHeaders().putSingle(Headers.NPO_TOOK, ((Result<?>) entity).getTook());
            }
        } finally {
            ExtraHeaders.remove();
        }


    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Redirector.REDIRECTS.set(new HashMap<>());
    }
}
