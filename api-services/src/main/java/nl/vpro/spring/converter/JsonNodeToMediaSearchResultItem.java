package nl.vpro.spring.converter;

import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.api.transfer.MediaSearchResultItem;
import nl.vpro.jackson.MediaMapper;
import org.codehaus.jackson.JsonNode;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Michiel Meeuwissen
 * @since 1.7
 */
public class JsonNodeToMediaSearchResultItem implements Converter<JsonNode, MediaSearchResultItem> {

    @Override
    public MediaSearchResultItem convert(JsonNode source) {
        String urn = source.get("urn").getTextValue();
        MediaObject mo = MediaMapper.convert(urn, source);
        return new MediaSearchResultItem(mo);
    }
}
