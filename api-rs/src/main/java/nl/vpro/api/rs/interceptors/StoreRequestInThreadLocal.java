package nl.vpro.api.rs.interceptors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.*;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.*;

import org.apache.commons.io.IOUtils;

/**
 * @author Michiel Meeuwissen
 * @since 4.6
 */
@Provider
@PreMatching
public class StoreRequestInThreadLocal implements ContainerRequestFilter, ContainerResponseFilter, WriterInterceptor {

    public static final ThreadLocal<byte[]> REQUEST = new ThreadLocal<>();
    public static final ThreadLocal<MultivaluedMap <String, String>> HEADERS= new ThreadLocal<>();

    public static String getRequestBody() {
        byte[] body = REQUEST.get();
        return HEADERS.get() + (body == null ? "" : ("\n" + new String(body)));
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        context.proceed();
        REQUEST.remove();
        HEADERS.remove();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IOUtils.copy(requestContext.getEntityStream(), bytes);
        REQUEST.set(bytes.toByteArray());
        HEADERS.set(requestContext.getHeaders());
        requestContext.setEntityStream(new ByteArrayInputStream(bytes.toByteArray()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // This is called before exceptions handlers.
        //REQUEST.remove();
        //HEADERS.remove();
    }
}
