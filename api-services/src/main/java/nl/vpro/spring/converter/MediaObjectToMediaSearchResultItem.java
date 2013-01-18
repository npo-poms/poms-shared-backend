package nl.vpro.spring.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;

import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.service.MediaServiceImpl;
import nl.vpro.api.transfer.Location;
import nl.vpro.api.transfer.MediaSearchResultItem;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public class MediaObjectToMediaSearchResultItem implements Converter<MediaObject, MediaSearchResultItem> {


    @Override
    public MediaSearchResultItem convert(MediaObject source) {
        MediaSearchResultItem value = new MediaSearchResultItem();
        value.setAvType(source.getAvType().toString());
        value.setBroadcaster(source.getBroadcasters());
        value.setCreationDate(source.getCreationDate());
        value.setLastModified(source.getLastModified());

        if (source.getDescriptions().size() > 0) {
            value.setDescription(source.getDescriptions().get(0).getValue());
        }
        if (source.getTitles().size() > 0) {
            value.setTitle(source.getTitles().get(0).getValue());
        }
        if (source.getDuration() != null) {
            value.setDuration(source.getDuration().getTime());
        }
        value.setScore(1f);
        value.setTags(source.getTags());
        value.setUrn(source.getUrn());
        List<Location> locations = new ArrayList<Location>();
        addLocations(source, locations);
        if (source instanceof Segment) {
            // Hmm. this is a bit sad. We just need the format to make FilteringIterator work.
            Program program = MediaServiceImpl.INSTANCE.getProgram(MediaUtil.getMediaId(MediaObjectType.program, ((Segment) source).getUrnRef()));
            addLocations(program, locations);
        }
        value.setLocations(locations);

        // TODO not sure all field are copied now, but more is not needed right now in any case
        return value;
    }

    private void addLocations(MediaObject source, List<Location> list) {
        for (nl.vpro.api.domain.media.Location o : source.getLocations()) {
            Location loc = new Location();
            loc.setAvFileFormat(o.getAvAttributes().getAvFileFormat().toString());
            loc.setProgramUrl(o.getProgramUrl());
            list.add(loc);
        }
    }
}
