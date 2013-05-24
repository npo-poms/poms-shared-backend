package nl.vpro.api.cors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author rico
 * @author Michiel Meeuwissen
 */
@Provider
public class CorsInterceptor implements ContainerResponseFilter {

    private final CorsPolicy corsPolicy;

    @Autowired
    CorsInterceptor(CorsPolicy policy) {
        this.corsPolicy = policy;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext context) throws IOException {
        String origin = requestContext.getHeaderString(CorsHeaders.ORIGIN);
        if (corsPolicy.isEnabled()) {
            String method = requestContext.getMethod();
            if (StringUtils.isNotEmpty(origin)) {
                boolean allowed = corsPolicy.allowedOriginAndMethod(origin, method);
                if (allowed) {
                    context.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                    //ACCESS_CONTROL_ALLOW_ORIGIN_VALUE is ook beschikbaar
                    context.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS, CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS_VALUE);
                    context.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS, CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS_VALUE);
                }
            }
        } else {
            if (StringUtils.isNotEmpty(origin)) {
                context.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }
        }

    }

}
