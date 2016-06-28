package nl.vpro.api.rs.v3.interceptors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.*;

import org.apache.commons.io.IOUtils;

/**
 * @author Michiel Meeuwissen
 * @since 4.6
 */
@Provider
public class StoreRequestInThreadLocal implements ReaderInterceptor, WriterInterceptor {

    public static final ThreadLocal<byte[]> REQUEST = new ThreadLocal<>();
    public static final ThreadLocal<MultivaluedMap <String, String>> HEADERS= new ThreadLocal<>();

    public static String getRequestBody() {
        return String.valueOf(HEADERS.get()) + "\n" + new String(REQUEST.get());
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IOUtils.copy(context.getInputStream(), bytes);
        REQUEST.set(bytes.toByteArray());
        HEADERS.set(context.getHeaders());
        context.setInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        return context.proceed();
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        context.proceed();
        REQUEST.remove();
        HEADERS.remove();

    }
}
