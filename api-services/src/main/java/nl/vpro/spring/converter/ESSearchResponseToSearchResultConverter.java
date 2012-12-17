package nl.vpro.spring.converter;

import nl.vpro.api.domain.media.ImageType;
import nl.vpro.api.domain.media.TextualType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.service.search.es.SearchResponseExtender;
import nl.vpro.api.transfer.Broadcasting;
import nl.vpro.api.transfer.Location;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchResultItem;
import nl.vpro.jackson.MediaMapper;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: ernst
 * Date: 10/2/12
 * Time: 2:59 PM
 */
public class ESSearchResponseToSearchResultConverter implements Converter<SearchResponseExtender, MediaSearchResult> {
    private String img_url_template;

    @Override
    public MediaSearchResult convert(SearchResponseExtender responseExtender) {
        final SearchResponse response = responseExtender.searchResponse();
        final int start = responseExtender.start() == null ? 0 : responseExtender.start();

        MediaSearchResult mediaSearchResult = new MediaSearchResult(response.hits().totalHits(), start, response.hits().maxScore());
        MediaSearchResultItem item;
        for (SearchHit hit : response.getHits()) {
            item = new MediaSearchResultItem();
            item.setScore(hit.score());
            item.setUrn(hit.id());
            try {
                JsonNode node = new MediaMapper().readTree(hit.sourceAsString());
                JsonNode titles = node.get("titles");
                item.setTitle(getStringFromArray(titles, TextualType.MAIN));
                JsonNode descriptions = node.get("descriptions");
                item.setDescription(getStringFromArray(descriptions, TextualType.MAIN, TextualType.SHORT));
                JsonNode images = node.get("images");
                item.setImageUrn(getStringFromArray(images, "imageUri", ImageType.STILL, ImageType.PICTURE, ImageType.PORTRAIT, ImageType.LOGO, ImageType.ICON, ImageType.BACKGROUND));
                item.setImageUrl(createUrlFromUrn(item.getImageUrn()));
                JsonNode broadcasters = node.get("broadcasters");
                item.setBroadcaster(getListFromArray(broadcasters));
                JsonNode genres = node.get("genres");
                item.setGenre(getListFromArray(genres));
                item.setAvType(node.get("avType").getTextValue());
                if (node.has("duration")) {
                    item.setDuration(node.get("duration").getLongValue() * 1000L);
                }
                if (node.has("type")) {
                    item.setMediaType(node.get("type").getTextValue());
                } else {
                    item.setMediaType(MediaUtil.getMediaType(item.getUrn()).name());
                }
                if (node.has("creationDate")) {
                    item.setCreationDate(new Date(node.get("creationDate").getLongValue()));
                }
                JsonNode events = node.get("scheduleEvents");
                setFirstBroadcast(events, item);
                JsonNode locations = node.get("locations");
                setLocations(locations, item);
                JsonNode tags = node.get("tags");
                item.setTags(getListFromArray(tags));

                mediaSearchResult.addMediaSearchResultItem(item);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return mediaSearchResult;
    }

    private String getStringFromArray(JsonNode node, TextualType... types) {
        return getStringFromArray(node, "value", types);
    }

    private String getStringFromArray(JsonNode node, String field, TextualType... types) {
        String value = "";
        boolean found = false;
        if (node != null && node.isArray()) {
            for (TextualType type : types) {
                for (JsonNode entry : node) {
                    if (entry.get("type").getTextValue().equals(type.name())) {
                        value = entry.get(field).getTextValue();
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }
        return value;
    }

    private String getStringFromArray(JsonNode node, String field, ImageType... types) {
        String value = null;
        boolean found = false;
        if (node != null && node.isArray()) {
            for (ImageType type : types) {
                for (JsonNode entry : node) {
                    if (entry.get("type").getTextValue().equals(type.name())) {
                        value = entry.get(field).getTextValue();
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }
        return value;
    }

    private List<String> getListFromArray(JsonNode node) {
        List<String> list = new ArrayList<String>();
        if (node != null && node.isArray()) {
            for (JsonNode entry : node) {
                if (entry.isTextual()) {
                    list.add(entry.getTextValue());
                }
            }
        }
        return list;
    }

    private void setFirstBroadcast(JsonNode arrayNode, MediaSearchResultItem item) {
        Long broadcastStart = Long.MAX_VALUE;
        String broadcastChannel = null;

        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                long start = node.get("start").getLongValue();
                String channel = node.get("channel").getTextValue();
                if (start < broadcastStart) {
                    broadcastStart = start;
                    broadcastChannel = channel;
                }
            }
            if (broadcastChannel != null) {
                item.setFirstBroadcasting(new Broadcasting(broadcastChannel, new Date(broadcastStart)));
            }
        }
    }

    private void setLocations(JsonNode arrayNode, MediaSearchResultItem item) {
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                String url = node.get("programUrl").getTextValue();
                String format = node.get("avAttributes").get("avFileFormat").getTextValue();
                item.addLocation(new Location(format, url));
            }
        }
    }

    private String createUrlFromUrn(String urn) {
        if (StringUtils.isNotEmpty(urn)) {
            String id = StringUtils.substringAfterLast(urn, ":");
            return img_url_template.replace("{id}", id);
        } else {
            return null;
        }
    }

    public void setImg_url_template(String img_url_template) {
        this.img_url_template = img_url_template;
    }
}
