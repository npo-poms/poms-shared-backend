package nl.vpro.api.util;

import java.util.Iterator;

import org.codehaus.jackson.JsonNode;

import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.jackson.MediaMapper;
import nl.vpro.util.WrappedIterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public class MediaObjectIterator extends WrappedIterator<JsonNode, MediaObject> {

    public MediaObjectIterator(Iterator<JsonNode> wrapped){
        super(wrapped);
    }

    @Override
    public MediaObject next() {
        JsonNode node = wrapped.next();
        String urn = node.get("urn").getTextValue();
        return MediaMapper.convert(urn, node);
    }
}
