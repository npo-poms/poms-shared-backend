package nl.vpro.api.rs.bodywriters;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

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
