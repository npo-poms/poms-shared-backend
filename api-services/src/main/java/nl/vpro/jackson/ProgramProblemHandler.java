package nl.vpro.jackson;

import nl.vpro.domain.media.support.Description;
import nl.vpro.domain.media.support.Title;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.DeserializationProblemHandler;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;

/**
 * Date: 27-3-12
 * Time: 9:05
 *
 * @author Ernst Bunders
 */
public class ProgramProblemHandler extends DeserializationProblemHandler {


    @Override
    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
        if ("value".equals(propertyName)) {
            if(beanOrClass.getClass().equals(Description.class)) {
                ((Description) beanOrClass).setDescription(ctxt.getParser().getText());
            }
            if(beanOrClass.getClass().equals(Title.class)) {
                ((Title) beanOrClass).setTitle(ctxt.getParser().getText());
            }
        }
        return super.handleUnknownProperty(ctxt, deserializer, beanOrClass, propertyName);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
