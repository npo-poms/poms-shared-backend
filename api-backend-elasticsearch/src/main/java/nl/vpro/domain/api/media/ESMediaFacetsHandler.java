/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import nl.vpro.domain.api.ESFacetsBuilder;
import nl.vpro.domain.api.ESFacetsHandler;
import nl.vpro.domain.api.MultipleFacetsResult;
import nl.vpro.domain.api.TermFacetResultItem;
import nl.vpro.domain.media.*;

import static nl.vpro.domain.api.ESFacetsBuilder.getAggregationName;
import static nl.vpro.domain.api.ESFacetsBuilder.getNestedFieldName;
import static nl.vpro.domain.api.media.ESMediaFacetsBuilder.ROOT_FILTER;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Slf4j
public class ESMediaFacetsHandler extends ESFacetsHandler {

    public static MediaFacetsResult extractMediaFacets(
        @Nonnull SearchResponse response,
        @Nullable MediaFacets facets,
        @Nonnull MediaLoader mediaRepository) {
        return extractMediaFacets("", response, facets, mediaRepository);
    }

    @Nullable
    public static MediaFacetsResult extractMediaFacets(
        @Nonnull String prefix,
        @Nonnull SearchResponse response,
        @Nullable MediaFacets request,
        @Nonnull MediaLoader mediaRepository) {
        if (request == null || !request.isFaceted()) {
            return null;

        }

        MediaFacetsResult facetsResult = new MediaFacetsResult();

        Aggregations responseAggregations = response.getAggregations();
        if (responseAggregations != null) {
            Filter rootFilter = responseAggregations.get(ROOT_FILTER);

            facetsResult.setTitles(
                getTitleAggregationResultItems(request.getTitles(), rootFilter)
            );
            List<TermFacetResultItem> titles = facetsResult.getTitles();
            StringTerms terms = rootFilter.getAggregations().get(getAggregationName(prefix, "titles.value.full"));
            if (terms != null) {
                if (titles == null) {
                    titles = new ArrayList<>();
                }
                for (StringTerms.Bucket bucket : terms.getBuckets()) {
                    titles.add(new TermFacetResultItem(bucket.getKeyAsString(), bucket.getKeyAsString(), bucket.getDocCount()));
                }

            }


            facetsResult.setTypes(getFacetResultItemsForEnum(prefix, "type", request.getTypes(), rootFilter, MediaType.class));
            facetsResult.setAvTypes(getFacetResultItemsForEnum(prefix, "avType", request.getAvTypes(), rootFilter, AVType.class));
            facetsResult.setSortDates(getDateRangeFacetResultItems(request.getSortDates(), prefix + "sortDate", response));
            facetsResult.setBroadcasters(
                filterThreshold(
                    getBroadcasterResultItems(prefix, "broadcasters.id", rootFilter), request.getBroadcasters()));
            facetsResult.setTags(getFacetResultItems(prefix, "tags", rootFilter, request.getTags()));
            facetsResult.setDurations(getDurationRangeFacetResultItems(request.getDurations(), prefix + "duration", response));
            facetsResult.setAgeRatings(getFacetResultItemsForEnum(prefix, "ageRating", request.getAgeRatings(), rootFilter, AgeRating.class, s -> AgeRating.xmlValueOf(s.toUpperCase()), AgeRating::getXmlValue));
            facetsResult.setContentRatings(getFacetResultItemsForEnum(prefix, "contentRatings", request.getContentRatings(), rootFilter, ContentRating.class));


            facetsResult.setGenres(getGenreAggregationResultItems(prefix, rootFilter));

            facetsResult.setDescendantOf(getMemberRefAggregationResultItems(prefix, "descendantOf", mediaRepository, rootFilter));
            facetsResult.setEpisodeOf(getMemberRefAggregationResultItems(prefix, "episodeOf", mediaRepository, rootFilter));
            facetsResult.setMemberOf(getMemberRefAggregationResultItems(prefix, "memberOf", mediaRepository, rootFilter));
            facetsResult.setRelations(getMediaRelationAggregationResultItems(prefix, request.getRelations(), rootFilter));

        }
        return facetsResult;
    }


    protected static List<GenreFacetResultItem> getGenreAggregationResultItems(
        @Nonnull String prefix,
        @Nullable Filter root) {
        if (root == null) {
            return null;
        }

        Terms ids = getNestedResult(prefix, root, () -> getNestedFieldName("genres", "id"));

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


    protected static List<MemberRefFacetResultItem> getMemberRefAggregationResultItems(
        @Nonnull  final String prefix,
        @Nonnull  final String nestedObject,
        @NonNull  final MediaLoader mediaRepository,
        @Nullable final Filter root) {
        if (root == null) {
            return null;
        }

        String nestedFacet = "midRef";

        Terms midRefs = getNestedResult(prefix, root,  () -> getNestedFieldName(nestedObject, "midRef"));

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

    protected static List<MultipleFacetsResult> getMediaRelationAggregationResultItems(
        @Nonnull String prefix,
        @Nullable RelationFacetList requestedRelations,
        @Nonnull Filter root) {
        if (requestedRelations == null) {
            return null;
        }

        List<MultipleFacetsResult> answer = new ArrayList<>();

        for (RelationFacet facet : requestedRelations) {

            Terms terms = ESFacetsHandler.getNestedResult(prefix,
                root,
                facet::getName);

            if (terms != null) {
                AggregationResultItemList<Terms, Terms.Bucket, TermFacetResultItem> resultItems = new AggregationResultItemList<Terms, Terms.Bucket, TermFacetResultItem>(terms) {
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

        return answer;


    }

    protected static List<TermFacetResultItem> getTitleAggregationResultItems(
        @Nullable TitleFacetList requestedTitles,
        @Nullable Filter root) {
        if (root == null || requestedTitles == null) {
            return null;
        }

        List<TermFacetResultItem> answer = new ArrayList<>();
        for (TitleFacet facet : requestedTitles) {
            String escapedFacetName = ESFacetsBuilder.escape(facet.getName());

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
