package nl.vpro.api.service.search;

import nl.vpro.domain.media.AVFileFormat;
import nl.vpro.domain.media.AVType;
import nl.vpro.domain.media.search.MediaType;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 19-3-12
 * Time: 13:51
 *
 * @author Ernst Bunders
 */
public class MediaSearchQuery {

    private List<MediaType> mediaTypes = new ArrayList<MediaType>();

    private List<AVFileFormat> locationFormats = new ArrayList<AVFileFormat>();

    private List<AVType> avTypes = new ArrayList<AVType>();

    private List<String> descendants = new ArrayList<String>();

    private String mainTitle;
    private String queryString;


    public MediaSearchQuery addMediaType(MediaType mediaType) {
        mediaTypes.add(mediaType);
        return this;
    }

    public MediaSearchQuery addLocationFormat(AVFileFormat avFileFormat) {
        locationFormats.add(avFileFormat);
        return this;
    }

    public MediaSearchQuery addAvType(AVType avType) {
        avTypes.add(avType);
        return this;
    }

    public MediaSearchQuery addDescendant(String descendant) {
        descendants.add(descendant);
        return this;
    }

    public MediaSearchQuery setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
        return this;
    }

    public MediaSearchQuery setQueryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

    public String createQueryString() {
        StringBuilder sb = new StringBuilder();

        for (MediaType mediaType : mediaTypes) {
            sb.append("mediaType:").append(mediaType.name()).append(" ");
        }

        for (AVFileFormat fileFormat : locationFormats) {
            sb.append("location_formats:").append(fileFormat.name()).append(" ");
        }

        for (AVType avType : avTypes) {
            sb.append("avType:").append(avType.name()).append(" ");
        }

        for (String descendant : descendants) {
            sb.append("descendantOf:").append(descendant).append(" ");
        }

        if (StringUtils.isNotBlank(mainTitle)) {
            sb.append("titleMain:").append(mainTitle).append(" ");
        }

        if (StringUtils.isNotBlank(queryString)) {
            sb.append(queryString);
        }

        return sb.toString().trim();
    }
}
