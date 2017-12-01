/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;
import nl.vpro.domain.api.ESFacetsBuilder;
import nl.vpro.domain.api.ESFacetsHandler;
import nl.vpro.domain.api.MultipleFacetsResult;
import nl.vpro.domain.api.TermFacetResultItem;
import nl.vpro.domain.media.*;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Slf4j
public class ESMediaFacetsHandler extends ESFacetsHandler {

    public static MediaFacetsResult extractFacets(SearchResponse response, MediaFacets facets, MediaLoader mediaRepository) {
        return extractFacets(response, facets, mediaRepository, "");
    }

    public static MediaFacetsResult extractFacets(SearchResponse response, MediaFacets request, MediaLoader mediaRepository, @Nonnull String prefix) {
        if (request == null || !request.isFaceted()) {
            return null;

        }

        MediaFacetsResult facetsResult = new MediaFacetsResult();

        Aggregations aggregations = response.getAggregations();
        if (aggregations != null) {
            Filter globalFilter = aggregations.get(ROOT_FILTER);

            facetsResult.setTitles(
                getTitleAggregationResultItems(request.getTitles(), globalFilter)
            );
            List<TermFacetResultItem> titles = facetsResult.getTitles();
            Aggregation aggregation = aggregations.get(prefix + "titles.value.full");
            if (aggregation != null) {
                if (titles == null) {
                    titles = new ArrayList<>();
                }
                StringTerms terms = (StringTerms) aggregation;
                for (StringTerms.Bucket bucket : terms.getBuckets()) {
                    titles.add(new TermFacetResultItem(bucket.getKeyAsString(), bucket.getKeyAsString(), bucket.getDocCount()));
                }

            }


            facetsResult.setTypes(getFacetResultItemsForEnum(prefix + "type", request.getTypes(), aggregations, MediaType.class));
            facetsResult.setTypes(getFacetResultItemsForEnum(prefix + "type", request.getTypes(), aggregations, MediaType.class));
            facetsResult.setAvTypes(getFacetResultItemsForEnum(prefix + "avType", request.getAvTypes(), aggregations, AVType.class));
            facetsResult.setSortDates(getDateRangeFacetResultItems(request.getSortDates(), prefix + "sortDate", response));
            facetsResult.setBroadcasters(filterThreshold(getBroadcasterResultItems(prefix + "broadcasters.id", aggregations), request.getBroadcasters()));
            facetsResult.setTags(getFacetResultItems(prefix + "tags", aggregations, request.getTags()));
            facetsResult.setDurations(getDurationRangeFacetResultItems(request.getDurations(), prefix + "duration", response));
            facetsResult.setAgeRatings(getFacetResultItemsForEnum(prefix + "ageRating", request.getAgeRatings(), aggregations, AgeRating.class, s -> AgeRating.xmlValueOf(s.toUpperCase()), AgeRating::getXmlValue));
            facetsResult.setContentRatings(getFacetResultItemsForEnum(prefix + "contentRatings", request.getContentRatings(), aggregations, ContentRating.class));


            facetsResult.setGenres(getGenreAggregationResultItems(prefix, globalFilter));

            facetsResult.setDescendantOf(getMemberRefAggregationResultItems(prefix, "descendantOf", mediaRepository, globalFilter));
            facetsResult.setEpisodeOf(getMemberRefAggregationResultItems(prefix, "episodeOf", mediaRepository, globalFilter));
            facetsResult.setMemberOf(getMemberRefAggregationResultItems(prefix, "memberOf", mediaRepository, globalFilter));
            facetsResult.setRelations(getRelationAggregationResultItems(request.getRelations(), globalFilter));

        }
        return facetsResult;
    }

    private static List<TermFacetResultItem> filterThreshold(List<TermFacetResultItem> list, MediaFacet facet) {
        if (facet == null || facet.getThreshold() == null) {
            return list;
        }
        return list.stream().filter(item -> item.getCount() >= facet.getThreshold()).collect(Collectors.toList());

    }

    protected static List<GenreFacetResultItem> getGenreAggregationResultItems(String prefix, Filter root) {
        if (root == null) {
            return null;
        }

        Aggregation nested = getNestedResult(prefix, "genres", root);

        Terms ids = getFilteredTerms("id", nested);
        if (ids == null) {
            return null;
        }

        return new AggregationResultItemList<Terms, Terms.Bucket, GenreFacetResultItem>(ids) {
            @Override
            protected GenreFacetResultItem adapt(Terms.Bucket bucket) {
                String id = bucket.getKeyAsString();
                Genre genre = new Genre(id);
                return new GenreFacetResultItem(
                    genre.getTerms(),
                    genre.getDisplayName(),
                    id,
                    bucket.getDocCount());
            }
        };
    }


    protected static List<MemberRefFacetResultItem> getMemberRefAggregationResultItems(String prefix, String nestedField, final MediaLoader mediaRepository, Filter root) {
        if (root == null) {
            return null;
        }

        Aggregation nested = getNestedResult(prefix, nestedField, root);

        Terms midRefs = getFilteredTerms("midRef", nested);
        if (midRefs == null) {
            return null;
        }

        return new AggregationResultItemList<Terms, Terms.Bucket, MemberRefFacetResultItem>(midRefs) {
            @Override
            protected MemberRefFacetResultItem adapt(Terms.Bucket bucket) {
                String mid = bucket.getKeyAsString();
                MediaObject owner = mediaRepository.findByMid(mid);
                return new MemberRefFacetResultItem(
                    owner != null ? owner.getMainTitle() : mid,
                    mid,
                    owner != null ? MediaType.getMediaType(owner) : null,
                    bucket.getDocCount());
            }
        };
    }

    protected static List<MultipleFacetsResult> getRelationAggregationResultItems(RelationFacetList requestedRelations, Filter root) {
        if (root == null || requestedRelations == null) {
            return null;
        }

        List<MultipleFacetsResult> answer = new ArrayList<>();

        for (RelationFacet facet : requestedRelations) {
            String escapedFacetName = ESFacetsBuilder.escapeFacetName(facet.getName());
            HasAggregations aggregations = root.getAggregations().get("filter_" + escapedFacetName);
            if (aggregations != null) {
                Aggregation aggregation = aggregations.getAggregations().get("filter_" + escapedFacetName);
                if (aggregation != null) {
                    Aggregation subAggregation = ((HasAggregations) aggregation).getAggregations().get("relations");
                    if (subAggregation == null) {
                        subAggregation = ((HasAggregations) aggregation).getAggregations().get("embeds_media_relations");
                    }
                    final Terms relations = getFilteredTerms(escapedFacetName, subAggregation);

                    if (relations != null) {
                        AggregationResultItemList<Terms, Terms.Bucket, TermFacetResultItem> resultItems = new AggregationResultItemList<Terms, Terms.Bucket, TermFacetResultItem>(relations) {
                            @Override
                            protected TermFacetResultItem adapt(Terms.Bucket bucket) {
                                return new TermFacetResultItem(
                                    bucket.getKeyAsString(),
                                    bucket.getKeyAsString(),
                                    bucket.getDocCount()
                                );
                            }
                        };
                        answer.add(new MultipleFacetsResult(facet.getName(), resultItems));
                    }
                }
            }
        }

        return answer;
    }

    protected static List<TermFacetResultItem> getTitleAggregationResultItems(TitleFacetList requestedTitles, Filter root) {
        if (root == null || requestedTitles == null) {
            return null;
        }

        List<TermFacetResultItem> answer = new ArrayList<>();
        for (TitleFacet facet : requestedTitles) {
            String escapedFacetName = ESFacetsBuilder.escapeFacetName(facet.getName());

            Aggregation aggregation = root.getAggregations().get(escapedFacetName);
            if (aggregation != null) {
                if (aggregation instanceof Filter) {
                    Filter filter = (Filter) aggregation;
                    TermFacetResultItem item = TermFacetResultItem.builder()
                        .count(filter.getDocCount())
                        .id(escapedFacetName)
                        .build();

                    answer.add(item);
                } else {
                    // backwards compatible
                    log.info("" + aggregation);
                }
            }
        }
        return answer;
    }

}
