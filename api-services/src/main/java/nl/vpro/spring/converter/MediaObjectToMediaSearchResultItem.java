package nl.vpro.spring.converter;

import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.service.MediaServiceImpl;
import nl.vpro.api.transfer.Location;
import nl.vpro.api.transfer.MediaSearchResultItem;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public class MediaObjectToMediaSearchResultItem implements Converter<MediaObject, MediaSearchResultItem> {


    @Override
    public MediaSearchResultItem convert(MediaObject source) {

        MediaSearchResultItem value = new MediaSearchResultItem(source);
        List<Location> locations = value.getLocations();
        if (source instanceof Segment) {
            // Hmm. this is a bit sad. We just need the format to make FilteringIterator work.
            Program program = MediaServiceImpl.INSTANCE.getProgram(MediaUtil.getMediaId(MediaObjectType.program, ((Segment) source).getUrnRef()));
            MediaSearchResultItem.addLocations(program, locations);
        }
        value.setLocations(locations);
        return value;
    }
}
