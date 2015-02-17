package nl.vpro.api.rs.v3.validation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import nl.vpro.domain.api.media.MediaForm;

/**
 * @author Michiel Meeuwissen
 * @since 3.4
 */
@Provider
public class MediaFormValidatingReader extends AbstractValidatingReader<MediaForm> {

    @Override
    Class<MediaForm> getClassToRead() {
        return MediaForm.class;

    }
}
