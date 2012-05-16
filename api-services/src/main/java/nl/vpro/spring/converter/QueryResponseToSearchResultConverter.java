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
            item.setBroadcaster(asList(solrDocument.getFieldValue("broadcaster")));
            item.setAvType((String) solrDocument.getFieldValue("avType"));
            item.setGenre(asList(solrDocument.getFieldValue("genre")));
            item.setMediaType((String) solrDocument.getFieldValue("mediaType"));
            item.setCreationDate(asDate(solrDocument.getFieldValue("creationDate")));
            Integer duration = asInteger(solrDocument.getFieldValue("duration"));
            if (duration != null) {
                //duration in solr is seconds, we want milliseconds
                item.setDuration(duration * 1000L);
            }

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
            Object next = solrDocument.getFieldValues(startField).iterator().next();
            firstBroadcastDate = asDate(next);
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

    private Date asDate(Object o) {
        if (o != null) {
            if (o instanceof Date) {
                return (Date) o;
            }
            if (o instanceof String) {
                try {
                    long l = Long.parseLong((String) o);
                    return new Date(l);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("You can not convert a string with value [" + o + "] to a date", e);
                }
            }
            throw new RuntimeException("You can not convert object of type [" + o.getClass() + "] to a date. It should be a string");
        }
        return new Date(0l);
    }

    private List<String> asList(Object o) {
        List<String> result = new ArrayList<String>();
        if (o instanceof Collection) {
            for (Object oo : (Collection) o) {
                result.add(oo.toString());
            }
        } else if (o != null) {
            result.add(o.toString());
        }
        return result;
    }

    private Integer asInteger(Object o) {
        if (o != null) {
            if (o instanceof Integer) {
                return (Integer) o;
            }
            if (o instanceof String) {
                try {
                    return Integer.parseInt((String) o);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("You can not convert String [" + (String) o + "] to an Iteger");
                }
            }
        }
        return null;
    }

    private String trimIfNotNull(String s) {
        if (s != null) {
            return s.trim();
        }
        return s;
    }
}
