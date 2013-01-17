package nl.vpro.api.util;

import java.util.Iterator;

import org.codehaus.jackson.JsonNode;

import com.google.common.collect.UnmodifiableIterator;

import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.jackson.MediaMapper;

/**
 * @author Michiel Meeuwissen
 * @since 1.8
 */
public class MediaObjectIterator extends UnmodifiableIterator<MediaObject> {

    private final Iterator<JsonNode> wrapped;

    public MediaObjectIterator(Iterator<JsonNode> wrapped){
        this.wrapped = wrapped;
    }

    @Override
    public boolean hasNext() {
        return wrapped.hasNext();
    }

    @Override
    public MediaObject next() {
        JsonNode node = wrapped.next();
        return getInstance(node);
    }

    private MediaObject getInstance(JsonNode node) {
        String urn = node.get("urn").getTextValue();
        return MediaMapper.convert(urn, node);
    }
}
