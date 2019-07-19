package nl.vpro.api.rs;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;

/**
 * @author Michiel Meeuwissen
 * @since 5.10
 */
@Provider
public class RSContextResolver implements ContextResolver<JAXBContext> {


    public static final JAXBContext ERROR_CONTEXT;
    static {
        try {
            ERROR_CONTEXT = JAXBContext.newInstance(
                nl.vpro.domain.api.Error.class,
                ResteasyConstraintViolation.class

            );
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public JAXBContext getContext(Class<?> type) {
        if (type.equals(nl.vpro.domain.api.Error.class)) {
            return ERROR_CONTEXT;
        } else {
            return null;
        }
    }
}
