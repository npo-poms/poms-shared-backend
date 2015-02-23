package nl.vpro.api.rs.v3.validation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.PostConstruct;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.xml.sax.SAXException;

/**
 * @author Michiel Meeuwissen
 * @since 3.4
 */
@Consumes("application/xml")
public abstract class AbstractValidatingReader<T> implements MessageBodyReader<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractValidatingReader.class);


    private final Unmarshaller unmarshaller;
    private final Class<T> classToRead;

    @Value("${xml.input.validate}")
    private boolean doValidate = true;

    public AbstractValidatingReader(Class<T> classToRead) {
        this.classToRead = classToRead;
        try {
            unmarshaller = JAXBContext.newInstance(this.classToRead).createUnmarshaller();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void init() throws JAXBException, IOException, SAXException {
        if (doValidate) {
            LOG.info("XML inputs for " + this.getClass().getName() + " will be validated (Setting xml.input.validate=true)");
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            JAXBContext context = JAXBContext.newInstance(this.classToRead);
            final DOMResult[] result = new DOMResult[1];
            result[0] = new DOMResult();
            context.generateSchema(new SchemaOutputResolver() {
                @Override
                public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                    result[0].setSystemId(namespaceUri);
                    return result[0];
                }
            });
            Schema schema = sf.newSchema(new DOMSource(result[0].getNode()));
            unmarshaller.setSchema(schema);
        } else {
            LOG.info("XML inputs for " + this.getClass().getName() + " will not be validated (Setting xml.input.validate=false)");
        }
    }
    public void setDoValidate(boolean doValidate) {
        this.doValidate = doValidate;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.isAssignableFrom(classToRead);
    }


    public final T unmarshal(InputStream inputStream) throws JAXBException {
        return unmarshaller.unmarshal(new StreamSource(inputStream), classToRead).getValue();
    }

    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            return unmarshal(entityStream);
        } catch (JAXBException e) {
            throw new BadRequestException(e);
        }
    }
}
