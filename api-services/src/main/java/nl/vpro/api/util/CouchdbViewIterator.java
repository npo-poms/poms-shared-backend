package nl.vpro.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.UnmodifiableIterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public class CouchdbViewIterator extends UnmodifiableIterator<JsonNode> {

    private static final Logger LOG = LoggerFactory.getLogger(CouchdbViewIterator.class);

    private final JsonParser parser;

    private JsonNode next;
    private int depth = 0;

    public CouchdbViewIterator(InputStream is) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory jsonFactory = mapper.getJsonFactory();
        this.parser = jsonFactory.createJsonParser(is);
    }

    @Override
    public boolean hasNext() {
        findNext();
        return next != null;
    }

    @Override
    public JsonNode next() {
        findNext();
        if (next == null) throw new NoSuchElementException();
        JsonNode result = next;
        next = null;
        return  result;
    }

    private void findNext() {
        if (next == null) {
            try {
                while (true) {
                    JsonToken token = parser.nextToken();
                    if (token == null) {
                        break;
                    }
                    if (token == JsonToken.START_OBJECT) {
                        depth++;
                    }
                    if (token == JsonToken.END_OBJECT) {
                        depth--;
                    }
                    if (token == JsonToken.START_OBJECT && depth == 3) {
                        next = parser.readValueAsTree();
                        depth--;
                        break;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


}
