package nl.vpro.api.rs.v3.validation;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import nl.vpro.domain.Xmlns;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.ScheduleForm;

/**
 * @author Michiel Meeuwissen
 * @since 4.2
 */
@Provider
@Slf4j
public class ScheduleFormValidatingReader extends AbstractValidatingReader<ScheduleForm> {


    MediaFormValidatingReader mediaFormValidatingReader = new MediaFormValidatingReader();

    public ScheduleFormValidatingReader() {
        super(ScheduleForm.class, Xmlns.API_NAMESPACE);
    }

    @Override
    public ScheduleForm readFrom(Class<ScheduleForm> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        ByteArrayOutputStream out = null;
        if (doValidate) {
            out = new ByteArrayOutputStream();
            IOUtils.copy(entityStream, out);
        }
        try {

            return unmarshal(out == null ? entityStream : new ByteArrayInputStream(out.toByteArray()));
        } catch (JAXBException e) {
            if (out != null) {
                try {
                    MediaForm form = mediaFormValidatingReader.unmarshal(new ByteArrayInputStream(out.toByteArray()));
                    log.warn("Received media form on schedule form post. Accepting for backwards compatibility");
                    return ScheduleForm.from(form);
                } catch (JAXBException e1) {
                    throw badRequestException(e);
                }
            } else {
                throw badRequestException(e);
            }

        }
    }

}
