package nl.vpro.spring.converter;

import nl.vpro.api.transfer.Broadcasting;
import nl.vpro.api.transfer.Location;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchResultItem;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.core.convert.converter.Converter;

import java.util.*;

/**
 * Date: 13-3-12
 * Time: 18:10
 *
 * @author Ernst Bunders
 */
public class QueryResponseToSearchResultConverter implements Converter<QueryResponse, MediaSearchResult> {
    private static final List<String> IMAGE_TYPES_ORDERED = Arrays.asList("LOGO", "STILL", "ICON", "BACKGROUND", "PORTRAIT", "PICTURE");
    private static final String IMG_URL_TEMPLATE = "http://poms-test.omroep.nl/images/ext-api/images/s100/{id}.jpg";

    @Override
    public MediaSearchResult convert(QueryResponse queryResponse) {
        SolrDocumentList sdl = queryResponse.getResults();
        MediaSearchResult searchResult = new MediaSearchResult(sdl.getNumFound(), sdl.getStart(), sdl.getMaxScore());
        MediaSearchResultItem item;

        for (Iterator<SolrDocument> it = sdl.iterator(); it.hasNext(); ) {
            SolrDocument solrDocument = it.next();
            item = new MediaSearchResultItem();
            item.setUrn(trimIfNotNull((String) solrDocument.getFieldValue("urn")));
            item.setTitle(trimIfNotNull((String) solrDocument.getFieldValue("titleMain")));
            item.setDescription(trimIfNotNull((String) solrDocument.getFieldValue("descriptionMain")));
            item.setScore((Float) solrDocument.getFieldValue("score"));
            item.setBroadcaster(concatenateListOfStrings((List<String>) solrDocument.getFieldValue("broadcasters"), ","));
            item.setAvType((String) solrDocument.getFieldValue("avType"));
            item.setGenre(concatenateListOfStrings((List<String>) solrDocument.getFieldValue("genre"), ","));
            item.setMediaType((String) solrDocument.getFieldValue("mediaType"));
            item.setCreationDate((Date) solrDocument.getFieldValue("creationDate"));
            item.setDuration((Integer) solrDocument.getFieldValue("duration"));

            setFirstBroadcastDate(solrDocument, item);
            setImageLink(solrDocument, item);

            setLocations(solrDocument, item);

            searchResult.addDocument(item);
        }
        return searchResult;
    }

    private void setLocations(SolrDocument solrDocument, MediaSearchResultItem item) {
        if (solrDocument.containsKey("location_formats")) {
            for (Object o : solrDocument.getFieldValues("location_formats")) {
                String locationFormatName = (String) o;
                if (solrDocument.containsKey("location_programUrl_" + locationFormatName)) {
                    for (Object location : solrDocument.getFieldValues("location_programUrl_" + locationFormatName)) {
                        item.addLocation(new Location(locationFormatName, (String) location));
                    }
                }
            }
        }
    }

    private String trimIfNotNull(String s) {
        if (s != null) {
            return s.trim();
        }
        return s;
    }

    private void setImageLink(SolrDocument solrDocument, MediaSearchResultItem item) {
        for (String imgType : IMAGE_TYPES_ORDERED) {
            String imageFieldName = "image_urn_" + imgType;
            if (solrDocument.getFieldNames().contains(imageFieldName)) {
                String imageUrn = (String) solrDocument.getFieldValues(imageFieldName).iterator().next();
                item.setImageUrl(createUrlFromUrn(imageUrn));
            }
        }
    }

    private void setFirstBroadcastDate(SolrDocument solrDocument, MediaSearchResultItem item) {
        String startField = "scheduleEvent_start";
        String channelField = "scheduleEvent_channel";

        Date firstBroadcastDate = null;
        if (solrDocument.getFieldNames().contains(startField)) {
            firstBroadcastDate = (Date) solrDocument.getFieldValues(startField).iterator().next();
        }

        String firstBroadcastChannel = null;
        if (solrDocument.getFieldNames().contains(channelField)) {
            firstBroadcastChannel = (String) solrDocument.getFieldValues(channelField).iterator().next();
        }

        if (firstBroadcastChannel != null && firstBroadcastDate != null) {
            item.setFirstBroadcasting(new Broadcasting(firstBroadcastChannel, firstBroadcastDate));
        }
    }

    private String createUrlFromUrn(String urn) {
        String id = StringUtils.substringAfterLast(urn, ":");
        return IMG_URL_TEMPLATE.replace("{id}", id);
    }

    private String concatenateListOfStrings(List<String> list, String separator) {
        if (list == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s).append(separator);
        }
        return StringUtils.substringBeforeLast(sb.toString(), separator);
    }
}
