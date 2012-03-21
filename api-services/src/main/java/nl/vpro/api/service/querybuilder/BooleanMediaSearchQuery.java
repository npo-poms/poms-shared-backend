package nl.vpro.api.service.querybuilder;

import nl.vpro.domain.media.AVFileFormat;
import nl.vpro.domain.media.AVType;
import nl.vpro.domain.media.search.MediaType;
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
public final class BooleanMediaSearchQuery extends MediaSearchQuery<BooleanMediaSearchQuery> {
    private static final Logger log = LoggerFactory.getLogger(BooleanMediaSearchQuery.class);
    private List<MediaType> mediaTypes = new ArrayList<MediaType>();

    private List<AVFileFormat> locationFormats = new ArrayList<AVFileFormat>();

    private List<AVType> avTypes = new ArrayList<AVType>();

    private List<String> descendants = new ArrayList<String>();

    private String mainTitle;

    private String documentType;

    public static final String DOCUMENT_TYPE_PROGRAM = "program";
    public static final String DOCUMENT_TYPE_GROUP = "group";

    public BooleanMediaSearchQuery(BooleanOp booleanOp) {
        super(booleanOp);
    }


    public BooleanMediaSearchQuery addMediaType(MediaType mediaType) {
        mediaTypes.add(mediaType);
        return this;
    }

    public BooleanMediaSearchQuery addLocationFormat(AVFileFormat avFileFormat) {
        locationFormats.add(avFileFormat);
        return this;
    }

    public BooleanMediaSearchQuery addAvType(AVType avType) {
        avTypes.add(avType);
        return this;
    }

    public BooleanMediaSearchQuery addDescendant(String descendant) {
        descendants.add(descendant);
        return this;
    }

    public BooleanMediaSearchQuery setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
        return this;
    }

    public BooleanMediaSearchQuery setDocumentType(String documentType) {
        this.documentType = documentType;
        return this;
    }


    public String createQueryString() {
        BooleanGroupingStringBuilder sb = new BooleanGroupingStringBuilder();

        for (MediaType mediaType : mediaTypes) {
            sb.append("mediaType:" + mediaType.name());
        }

        for (AVFileFormat fileFormat : locationFormats) {
            sb.append("location_formats:" + fileFormat.name());
        }

        for (AVType avType : avTypes) {
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


    @Override
    protected BooleanMediaSearchQuery getInstance() {
        return this;
    }


}
