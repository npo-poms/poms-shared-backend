package nl.vpro.api.cors;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.core.interception.PreMatchContainerRequestContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author rico
 * @author Michiel Meeuwissen
 */
@Provider
public class CorsInterceptor implements ContainerResponseFilter, ContainerRequestFilter {

    private final CorsPolicy corsPolicy;

    @Autowired
    CorsInterceptor(CorsPolicy policy) {
        this.corsPolicy = policy;
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        String origin = request.getHeaderString(CorsHeaders.ORIGIN);
        if(corsPolicy.isEnabled()) {
            String method = request.getMethod();
            if(StringUtils.isNotEmpty(origin)) {
                boolean allowed = corsPolicy.allowedOriginAndMethod(origin, method);

                if(allowed) {
                    response.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                    //ACCESS_CONTROL_ALLOW_ORIGIN_VALUE is ook beschikbaar
                    response.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS, CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS_VALUE);
                    response.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS, CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS_VALUE);
                    response.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
                }
            }
        } else {
            if(StringUtils.isNotEmpty(origin)) {
                response.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
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
            List<String> contentTypes = request.getHeaders().get("Content-Type");
            if(contentTypes == null) {
                request.getHeaders().add("Content-Type", "application/json");
            } else if(contentTypes.isEmpty() || contentTypes.equals(Arrays.asList("*/*")) || contentTypes.equals(Arrays.asList("text/plain"))) {
                contentTypes.clear();
                contentTypes.add("application/json");
            }
        }
    }
}
