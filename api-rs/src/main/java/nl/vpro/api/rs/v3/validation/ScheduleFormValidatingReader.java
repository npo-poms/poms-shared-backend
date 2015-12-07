package nl.vpro.api.rs.v3.validation;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.media.ScheduleForm;

/**
 * @author Michiel Meeuwissen
 * @since 4.2
 */
@Provider
public class ScheduleFormValidatingReader extends AbstractValidatingReader<ScheduleForm> {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduleFormValidatingReader.class);


    MediaFormValidatingReader mediaFormValidatingReader = new MediaFormValidatingReader();
    {
        mediaFormValidatingReader.setDoValidate(false);
    }

    public ScheduleFormValidatingReader() {
        super(ScheduleForm.class);
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
                    LOG.warn("Received media form on schedule form post. Accepting for backwards compatibility");
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
