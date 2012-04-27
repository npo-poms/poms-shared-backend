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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: ricojansen
 * Date: 27-04-2012
 * Time: 14:12
 * To change this template use File | Settings | File Templates.
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
        if (corsPolicy.isEnabled()) {
            String origin = httpServletRequest.getHeader(CorsHeaders.ORIGIN);
            String method = httpServletRequest.getMethod();
            if (StringUtils.isNotEmpty(origin)) {
                context.getHeaders().add("Access-Control-Allow-Origin", origin);
            }
        }
        context.proceed();
    }


}
