package nl.vpro.jackson;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.DeserializationProblemHandler;
import org.codehaus.jackson.map.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Date: 27-3-12
 * Time: 9:05
 *
 * @author Ernst Bunders
 */
public class MediaProblemHandler extends DeserializationProblemHandler {
    Logger logger = LoggerFactory.getLogger(MediaProblemHandler.class);


    @Override
    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
        if (beanOrClass != null) {
            Class<?> ref;
            if (beanOrClass instanceof Class<?>) {
                ref = (Class<?>) beanOrClass;
            } else {
                ref = beanOrClass.getClass();
            }
            String msg = "Unrecognized field \"" + propertyName + "\" (Class " + ref.getName() + "), found";
            logger.warn(msg);
            ctxt.getParser().skipChildren();
        }
        return true;
    }

}
