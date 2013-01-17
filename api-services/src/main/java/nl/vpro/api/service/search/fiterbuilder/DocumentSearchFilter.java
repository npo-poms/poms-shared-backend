package nl.vpro.api.service.search.fiterbuilder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import nl.vpro.api.domain.media.*;
import nl.vpro.api.domain.media.search.MediaType;

/**
 * Date: 20-3-12
 * Time: 16:00
 *
 * @author Ernst Bunders
 */
public final class DocumentSearchFilter extends SearchFilter<MediaObject, DocumentSearchFilter> {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentSearchFilter.class);

    private final Set<MediaType> mediaTypes         = new HashSet<MediaType>();

    private final Set<AvFileFormat> locationFormats = new HashSet<AvFileFormat>();

    private final Set<AvType> avTypes               = new HashSet<AvType>();

    private final Set<String> descendants           = new HashSet<String>();

    private final Set<String> broadcasters          = new HashSet<String>();

    private String mainTitle;

    private String documentType;

    public static final String DOCUMENT_TYPE_PROGRAM = "program";
    public static final String DOCUMENT_TYPE_GROUP = "group";

    public DocumentSearchFilter() {
        super(BooleanOp.AND);
    }


    public DocumentSearchFilter addMediaType(MediaType mediaType) {
        mediaTypes.add(mediaType);
        return this;
    }

    public DocumentSearchFilter addLocationFormat(AvFileFormat avFileFormat) {
        locationFormats.add(avFileFormat);
        return this;
    }

    public DocumentSearchFilter addAvType(AvType avType) {
        avTypes.add(avType);
        return this;
    }

    public DocumentSearchFilter addDescendant(String descendant) {
        descendants.add(descendant);
        return this;
    }

    public DocumentSearchFilter addBroadcaster(String broadcaster) {
        broadcasters.add(broadcaster);
        return this;
    }

    public DocumentSearchFilter setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
        return this;
    }

    public DocumentSearchFilter setDocumentType(String documentType) {
        this.documentType = documentType;
        return this;
    }

    @Override
    public boolean evaluate(MediaObject object) {
        if ("program".equals(this.documentType) && (!(object instanceof Program))) return false;
        if ("segment".equals(this.documentType) && (!(object instanceof Segment))) return false;
        if (! avTypes.isEmpty()) {
            if (! avTypes.contains(object.getAvType())) return false;
        }

        if (!locationFormats.isEmpty()) {
            if (object instanceof Program) {
                boolean found = false;
                for (Location location : object.getLocations()) {
                    if (locationFormats.contains(location.getAvAttributes().getAvFileFormat())) {
                        found = true;
                        break;
                    }
                }
                if (! found) return false;
            } else {
                // TODO
            }
        }
        if (! descendants.isEmpty()) {
            Set<String> descendantsOf = new HashSet<String>();
            for (DescendantRef ref : object.getDescendantOf()) {
                descendantsOf.add(ref.getUrnRef());
            }
            if ( Sets.intersection(descendants, descendantsOf).isEmpty()) return false;

        }
        if (!mediaTypes.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        if (! broadcasters.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        if (StringUtils.isNotEmpty(mainTitle)) {
            throw new UnsupportedOperationException();
        }

        return true;

    }


    @Override
    public String createQueryString() {
        BooleanGroupingStringBuilder sb = new BooleanGroupingStringBuilder();

        for (MediaType mediaType : mediaTypes) {
            sb.append("mediaType:" + mediaType.name());
        }

        for (AvFileFormat fileFormat : locationFormats) {
            sb.append("location_formats:" + fileFormat.name());
        }

        for (String broadcaster : broadcasters) {
            sb.append("broadcaster:" + broadcaster);
        }

        for (AvType avType : avTypes) {
            sb.append("avType:" + avType.name());
        }

        for (String descendant : descendants) {
            sb.append("descendantOf:" + wrapInQuotes(descendant));
        }

        if (StringUtils.isNotBlank(mainTitle)) {
            sb.append("titleMain:" + mainTitle);
        }

        if (StringUtils.isNotBlank(documentType)) {
            sb.append("documentType:" + documentType);
        }

        sb.close();

        if (StringUtils.isNotBlank(queryString)) sb.stringBuilder.append(queryString);

        String q = sb.toString();
        LOG.debug("query: " + q);
        return q;
    }

    public Collection<MediaType> getMediaTypes() {
        return mediaTypes;
    }

    public Collection<AvFileFormat> getLocationFormats() {
        return locationFormats;
    }

    public Collection<AvType> getAvTypes() {
        return avTypes;
    }

    public Collection<String> getDescendants() {
        return descendants;
    }

    public Collection<String> getBroadcasters() {
        return broadcasters;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public String getDocumentType() {
        return documentType;
    }

    @Override
    protected DocumentSearchFilter getInstance() {
        return this;
    }


}
