package nl.vpro.api.cors;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author rico
 * @author Michiel Meeuwissen
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class CorsInterceptor implements ContainerResponseFilter, ContainerRequestFilter {

    private final CorsPolicy corsPolicy;

    @Inject
    CorsInterceptor(CorsPolicy policy) {
        this.corsPolicy = policy;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        String origin = request.getHeaderString(HttpHeaders.ORIGIN);
        HttpServletResponse realResponse = ResteasyProviderFactory.getContextData(HttpServletResponse.class);
        boolean alreadyHasCorsHeaders = realResponse.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) != null;
        if  (!alreadyHasCorsHeaders) {
            if (corsPolicy.isEnabled()) {
                String method = request.getMethod();
                if (StringUtils.isNotEmpty(origin)) {
                    boolean allowed = corsPolicy.allowedOriginAndMethod(origin, method);

                    if (allowed) {
                        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                        //ACCESS_CONTROL_ALLOW_ORIGIN_VALUE is ook beschikbaar
                        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS_VALUE);
                        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS_VALUE);
                        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
                    }
                }
            } else {
                if (StringUtils.isNotEmpty(origin)) {
                    response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                }
            }
        }
    }


    /**
     * Hack for IE 8 . See http://blogs.msdn.com/b/ieinternals/archive/2010/05/13/xdomainrequest-restrictions-limitations-and-workarounds.aspx
     * (so this is related to cors)
     */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {

        if("POST".equals(request.getMethod())) {
            List<String> contentTypes = request.getHeaders().get(HttpHeaders.CONTENT_TYPE);
            if(contentTypes == null) {
                request.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            } else if(contentTypes.isEmpty() || contentTypes.equals(Collections.singletonList(MediaType.ALL_VALUE)) || contentTypes.equals(Collections.singletonList(MediaType.TEXT_PLAIN_VALUE))) {
                contentTypes.clear();
                contentTypes.add(MediaType.APPLICATION_JSON_VALUE);
            }
        }
    }
}
