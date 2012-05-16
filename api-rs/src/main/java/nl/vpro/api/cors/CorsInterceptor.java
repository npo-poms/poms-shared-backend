package nl.vpro.api.cors;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.interception.Precedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.spi.interception.MessageBodyWriterContext;
import org.jboss.resteasy.spi.interception.MessageBodyWriterInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * User: ricojansen
 * Date: 27-04-2012
 * Time: 14:12
 */
@Provider
@ServerInterceptor
@Precedence("HEADER_DECORATOR")
public class CorsInterceptor implements MessageBodyWriterInterceptor {
    @Autowired
    CorsPolicy corsPolicy;

    @Context
    private HttpServletRequest httpServletRequest;

    @Override
    public void write(MessageBodyWriterContext context) throws IOException, WebApplicationException {
        String origin = httpServletRequest.getHeader(CorsHeaders.ORIGIN);
        if (corsPolicy.isEnabled()) {
            String method = httpServletRequest.getMethod();
            if (StringUtils.isNotEmpty(origin)) {
                boolean allowed = corsPolicy.allowedOriginAndMethod(origin, method);
                if (allowed) {
                    context.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                }
            }
        } else {
            if (StringUtils.isNotEmpty(origin)) {
                context.getHeaders().add(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            }
        }
        context.proceed();
    }


}
