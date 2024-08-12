package nl.vpro.api.cors;

import java.util.Collections;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.jboss.resteasy.spi.CorsHeaders.*;


/**
 * @author rico
 * @author Michiel Meeuwissen
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class CorsInterceptor implements ContainerResponseFilter, ContainerRequestFilter {

    private final CorsPolicy corsPolicy;

    @Inject
    CorsInterceptor(CorsPolicy policy) {
        this.corsPolicy = policy;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        String origin = request.getHeaderString(ORIGIN);
        HttpServletResponse realResponse = ResteasyProviderFactory.getInstance().getContextData(HttpServletResponse.class);
        boolean alreadyHasCorsHeaders = realResponse.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN) != null;
        if  (!alreadyHasCorsHeaders) {
            if (corsPolicy.isEnabled()) {
                String method = request.getMethod();
                if (StringUtils.isNotEmpty(origin)) {
                    boolean allowed = corsPolicy.allowedOriginAndMethod(origin, method);

                    if (allowed) {
                        response.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                        //ACCESS_CONTROL_ALLOW_ORIGIN_VALUE is ook beschikbaar
                        response.getHeaders().add(ACCESS_CONTROL_ALLOW_METHODS, CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS_VALUE);
                        response.getHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS, CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS_VALUE);
                        response.getHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
                    }
                }
            } else {
                if (StringUtils.isNotEmpty(origin)) {
                    response.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                }
            }
        }
    }


    /**
     * Hack for IE 8 . See http://blogs.msdn.com/b/ieinternals/archive/2010/05/13/xdomainrequest-restrictions-limitations-and-workarounds.aspx
     * (so this is related to cors)
     */
    @Override
    public void filter(ContainerRequestContext request) {

        if("POST".equals(request.getMethod())) {
            List<String> contentTypes = request.getHeaders().get(CONTENT_TYPE);
            if(contentTypes == null) {
                request.getHeaders().add(CONTENT_TYPE, "application/json");
            } else if(contentTypes.isEmpty() || contentTypes.equals(Collections.singletonList("*/*")) || contentTypes.equals(Collections.singletonList("text/plain"))) {
                contentTypes.clear();
                contentTypes.add("application/json");
            }
        }
    }
}
