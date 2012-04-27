package nl.vpro.api.cors;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.spi.interception.MessageBodyWriterContext;
import org.jboss.resteasy.spi.interception.MessageBodyWriterInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: ricojansen
 * Date: 27-04-2012
 * Time: 14:12
 * To change this template use File | Settings | File Templates.
 */
@Provider
@ServerInterceptor
public class CorsInterceptor implements MessageBodyWriterInterceptor {

    @Context
    private HttpServletRequest httpServletRequest;

    @Override
    public void write(MessageBodyWriterContext context) throws IOException, WebApplicationException {
        String origin = httpServletRequest.getHeader("Origin");
        if (StringUtils.isNotEmpty(origin)) {
            context.getHeaders().add("Access-Control-Allow-Origin", origin);
        }
        context.proceed();
    }
}
