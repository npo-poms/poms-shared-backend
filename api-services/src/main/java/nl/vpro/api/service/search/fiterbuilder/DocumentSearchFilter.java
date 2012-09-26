package nl.vpro.api.service.search.fiterbuilder;

import nl.vpro.api.domain.media.AvFileFormat;
import nl.vpro.api.domain.media.AvType;
import nl.vpro.api.domain.media.search.MediaType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 20-3-12
 * Time: 16:00
 *
 * @author Ernst Bunders
 */
public final class DocumentSearchFilter extends SearchFilter<DocumentSearchFilter> {
    private static final Logger log = LoggerFactory.getLogger(DocumentSearchFilter.class);
    private List<MediaType> mediaTypes = new ArrayList<MediaType>();

    private List<AvFileFormat> locationFormats = new ArrayList<AvFileFormat>();

    private List<AvType> avTypes = new ArrayList<AvType>();

    private List<String> descendants = new ArrayList<String>();

    private List<String> broadcasters = new ArrayList<String>();

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
        log.debug("query: " + q);
        return q;
    }

    public List<MediaType> getMediaTypes() {
        return mediaTypes;
    }

    public List<AvFileFormat> getLocationFormats() {
        return locationFormats;
    }

    public List<AvType> getAvTypes() {
        return avTypes;
    }

    public List<String> getDescendants() {
        return descendants;
    }

    public List<String> getBroadcasters() {
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
