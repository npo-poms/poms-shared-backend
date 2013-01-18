package nl.vpro.api.service.search.fiterbuilder;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import nl.vpro.api.domain.media.*;
import nl.vpro.api.domain.media.search.MediaType;
import nl.vpro.api.domain.media.support.MediaObjectType;

/**
 * MM: Would it not be nicer to split up this class in several ones?
 *     E.g. a MediaTypeSearchFilter, LocationFormatSearchFilter, AVTypeSearchFilter.
 *     they can simply be joined with a SearchFilterList(AND).
 *     This would result in less clutter and a better seperation of concerns.
 *     Also I'd then propose to remove the BooleanOp from SearchFilter itself then, since that would
 *     be the task of SearchFilterList only.
 *
 * Date: 20-3-12
 * Time: 16:00
 *
 * @author Ernst Bunders
 */
public final class DocumentSearchFilter extends SearchFilter<DocumentSearchFilter> {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentSearchFilter.class);

    private final Set<MediaType> mediaTypes         = new LinkedHashSet<MediaType>();

    private final Set<AvFileFormat> locationFormats = new LinkedHashSet<AvFileFormat>();

    private final Set<AvType> avTypes               = new LinkedHashSet<AvType>();

    private final Set<String> descendants           = new LinkedHashSet<String>();

    private final Set<String> broadcasters          = new LinkedHashSet<String>();

    private String mainTitle;

    private MediaObjectType documentType;

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

    public DocumentSearchFilter setDocumentType(MediaObjectType documentType) {
        this.documentType = documentType;
        return this;
    }

    @Override
    public boolean apply(Object o) {
        if (! (o instanceof MediaObject)) return false;
        MediaObject object = (MediaObject) o;

        if (this.documentType != null) {
            if (! this.documentType.isInstance(object)) return false;
        }
        if (! avTypes.isEmpty()) {
            if (! avTypes.contains(object.getAvType())) return false;
        }

        if (!locationFormats.isEmpty()) {
            if (object instanceof Program) {
                boolean found = false;
                if (object.getLocations() != null) {
                    for (Location location : object.getLocations()) {
                        if (locationFormats.contains(location.getAvAttributes().getAvFileFormat())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (! found) return false;
            } else {
                // Hmm, this is actually hardly implementable because the 'location_formats' are only available in solr.
                // TODO
            }
        }
        if (! descendants.isEmpty()) {
            Set<String> descendantsOf = new HashSet<String>();
            for (DescendantRef ref : object.getDescendantOf()) {
                descendantsOf.add(ref.getUrnRef());
            }
            if (Sets.intersection(descendants, descendantsOf).isEmpty()) return false;

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

        if (documentType != null) {
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

    public MediaObjectType getDocumentType() {
        return documentType;
    }

    @Override
    protected DocumentSearchFilter getInstance() {
        return this;
    }


}
