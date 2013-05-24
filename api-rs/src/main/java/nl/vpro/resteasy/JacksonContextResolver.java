package nl.vpro.resteasy;

import nl.vpro.domain.media.MediaObject;
import nl.vpro.jackson.MediaMapper;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonContextResolver implements ContextResolver<ObjectMapper> {

    public JacksonContextResolver() throws Exception {

    }

    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        if (MediaObject.class.isAssignableFrom(objectType)) {
            return MediaMapper.INSTANCE;
        } else {
            return MediaMapper.INSTANCE;
        }
    }
}

