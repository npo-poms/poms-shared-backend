package nl.vpro.jackson;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.deser.StdDeserializer;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.io.IOException;

/**
 * Date: 04-04-2012
 * Time: 10:30
 *
 * @author Rico Jansen
 */
public class DurationDeserializer extends StdDeserializer<Duration> {
    final static DatatypeFactory _dataTypeFactory;

    static {
        try {
            _dataTypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public DurationDeserializer() {
        super(Duration.class);
    }

    @Override
    public Duration deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
            String text = jp.getText().trim();
            if (text.length() == 0) {
                return null;
            }
            try {
                Duration result = _dataTypeFactory.newDuration(text);
                if (result != null) {
                    return result;
                }
            } catch (IllegalArgumentException iae) {
            }
            throw ctxt.weirdStringException(_valueClass, "not a valid textual representation");
        } else if (jp.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
            try {
                Duration result = _dataTypeFactory.newDuration(jp.getIntValue());
                if (result != null) {
                    return result;
                }
            } catch (IllegalArgumentException iae) {
            }
        }

        throw ctxt.mappingException(_valueClass);
    }
}
