package nl.vpro.api.rs.bodywriters;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import nl.vpro.domain.api.Error;

/**
 * @author Michiel Meeuwissen
 * @since 3.7.1
 */
@Provider
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public class ErrorOctetstreamWriter implements MessageBodyWriter<nl.vpro.domain.api.Error> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Error.class.isAssignableFrom(type);

    }

    @Override
    public long getSize(Error error, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return error.toString().getBytes().length;

    }

    @Override
    public void writeTo(Error error, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(error.toString().getBytes());

    }
}
