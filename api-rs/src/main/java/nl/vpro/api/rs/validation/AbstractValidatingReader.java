package nl.vpro.api.rs.validation;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.factory.annotation.Value;
import org.xml.sax.SAXException;

import nl.vpro.api.util.ApiMappings;

/**
 * @author Michiel Meeuwissen
 * @since 3.4
 */
@Consumes("application/xml")
@Log4j2
public abstract class AbstractValidatingReader<T> implements MessageBodyReader<T> {

    public static final ThreadLocal<Boolean> CLIENT_PREFERENCE  = ThreadLocal.withInitial(() -> null);

    private final Class<T> classToRead;
    private final String namespace;

    @Inject
    private ApiMappings mappings;

    private ThreadLocal<Unmarshaller> unmarshaller;

    @Value("${xml.input.validate}")
    protected boolean doValidate = true;


    public AbstractValidatingReader(Class<T> classToRead, String namespace) {
        this(classToRead, namespace, null);
    }

    public AbstractValidatingReader(Class<T> classToRead, String namespace, ApiMappings mappings) {
        this.classToRead = classToRead;
        this.namespace = namespace;
        this.mappings = mappings;
    }

    @PostConstruct
    public void init() throws JAXBException, IOException, SAXException {
        if (mappings == null) {
            log.info("No apimappings injected");
            mappings = new ApiMappings(null);
        }
        unmarshaller = mappings.getUnmarshaller(doValidate, this.namespace);
        if (doValidate) {
            log.info("XML inputs for " + this.getClass().getName() + " will be validated (Setting xml.input.validate=true)");
        } else {
            log.info("XML inputs for " + this.getClass().getName() + " will not be validated (Setting xml.input.validate=false)");
        }
    }
    @PreDestroy
    public void shutdown() {
        unmarshaller.remove();
    }

    public void setDoValidate(boolean doValidate) {
        this.doValidate = doValidate;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.isAssignableFrom(classToRead);
    }


    public final T unmarshal(InputStream inputStream) throws JAXBException {
        return unmarshaller.get().unmarshal(new StreamSource(inputStream), classToRead).getValue();
    }

    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            return unmarshal(entityStream);
        } catch (JAXBException e) {
            throw badRequestException(e);
        }
    }

    protected BadRequestException  badRequestException(JAXBException e) {
        Throwable c = e.getLinkedException();
        if (c == null) {
            c = e;
        }
        log.warn(c.getMessage());
        return new BadRequestException(c.getMessage(), e);
    }
}
