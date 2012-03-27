package nl.vpro.jackson;

import nl.vpro.domain.media.Repeat;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.deser.StdDeserializer;
import org.codehaus.jackson.type.JavaType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 27-3-12
 * Time: 13:57
 *
 * @author Ernst Bunders
 */
public class RepeatDeserializer extends StdDeserializer<Repeat> {
    
    public RepeatDeserializer() {
        super(Repeat.class);
    }

    @Override
    public Repeat deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String value = null;
        boolean rerun = false;
        JsonToken token = jp.nextToken();
        while (token != JsonToken.END_OBJECT) {
            if (token == JsonToken.FIELD_NAME) {
                if (jp.getText() == "isRerun") {
                    jp.nextToken();
                    rerun = _parseBooleanPrimitive(jp, ctxt);
                }
                if (jp.getText() == "value") {
                    jp.nextToken();
                    value = jp.getText();
                }
            }
            token = jp.nextToken();
        }




        return new Repeat(rerun, value);
    }
}
