/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import nl.vpro.domain.api.*;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Slf4j
public class ESMediaFacetsBuilder extends ESFacetsBuilder {

    protected static final String ROOT_FILTER = "mediaRootFilter";



    /**
     * @param prefix empty string or path to this field including the last dot e.g., "embeds.media."
     */
    public static void buildMediaFacets(
        @Nonnull String prefix,
        @Nonnull SearchSourceBuilder searchBuilder,
        @Nullable MediaForm form,
        @Nonnull BoolQueryBuilder facetFilter) {
        if (form != null && form.isFaceted()) {

            MediaFacets facets = form.getFacets();

            // Under default ES behaviour profile filtering does not influence facet results
            if (facets.getFilter() != null) {
                MediaSearch search = facets.getFilter();
                QueryBuilder query = ESMediaQueryBuilder.query(prefix, search);

            }

            // Append all aggregations to this filtered aggregation builder
            FilterAggregationBuilder rootAggregation = AggregationBuilders
                .filter(ROOT_FILTER, facetFilter);

            searchBuilder.aggregation(rootAggregation);

            {
                TitleFacetList titles = facets.getTitles();
                if (titles != null) {
                    if (titles.asMediaFacet()) {
                        addMediaFacet(prefix, rootAggregation, "titles.value.full", titles);
                    }
                    addNestedTitlesAggregations(prefix, rootAggregation, titles);
                }

            }

            addMediaFacet(prefix, rootAggregation, "type", facets.getTypes());

            addMediaFacet(prefix, rootAggregation, "avType", facets.getAvTypes());

            addDateRangeFacet(prefix, rootAggregation, "sortDate", facets.getSortDates());

            addMediaFacet(prefix, rootAggregation, "broadcasters.id", facets.getBroadcasters());

            addMediaNestedAggregation(prefix, rootAggregation, "genres", "id", facets.getGenres());

            {
                ExtendedMediaFacet tags = facets.getTags();
                //addFacetFilter(prefix, tags, facetRootFilter);
                addMediaFacet(prefix, rootAggregation, esExtendedTextField("tags", tags), tags);
            }
            addDurationFacet(prefix, rootAggregation, "duration", facets.getDurations());

            addNestedAggregationMemberRefSearch(prefix, rootAggregation, "descendantOf", "midRef", facets.getDescendantOf());

            addNestedAggregationMemberRefSearch(prefix, rootAggregation, "episodeOf", "midRef", facets.getEpisodeOf());

            addNestedAggregationMemberRefSearch(prefix, rootAggregation, "memberOf", "midRef", facets.getMemberOf());

            addMediaNestedRelationAggregations(prefix, rootAggregation, facets.getRelations());

            addMediaFacet(prefix, rootAggregation, "ageRating", facets.getAgeRatings());

            addMediaFacet(prefix, rootAggregation, "contentRatings",  facets.getContentRatings());

        }
    }

    protected static <S> void addMediaNestedAggregation(
        @Nonnull String pathPrefix,
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nonnull String nestedObject,
        @Nonnull String facetField,
        @Nullable SearchableLimitableFacet<MediaSearch, TermSearch> facet) {
        addNestedAggregation(
            pathPrefix,
            rootAggregation, nestedObject,
            facetField,
            facet,
            mediaSearch -> ESMediaFilterBuilder.filter(pathPrefix, mediaSearch),
            () -> null,
            ESMediaQueryBuilder::search
        );

    }

    protected static void addNestedAggregationMemberRefSearch(
        @Nonnull String pathPrefix,
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nonnull String nestedObject,
        @Nonnull String facetField,
        @Nullable SearchableLimitableFacet<MediaSearch, MemberRefSearch> facet) {
        addNestedAggregation(
            pathPrefix,
            rootAggregation, nestedObject,
            facetField,
            facet,
            mediaSearch -> ESMediaFilterBuilder.filter(pathPrefix, mediaSearch),
            () -> null,
            (memberRefSearch, nestedObject1, facetField1) -> ESMediaQueryBuilder.filter(memberRefSearch, nestedObject1)

        );

    }


    protected static void addMediaNestedRelationAggregations(
        @Nonnull String prefix,
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nullable RelationFacetList facets) {
        if (facets == null || facets.isEmpty()) {
            return;
        }

        RelationSearch subSearch = facets.getSubSearch();
        for (RelationFacet facet : facets) {
            addMediaNestedRelationAggregation(
                prefix,
                rootAggregation, "relations",
                esExtendedTextField("value", facet),
                subSearch,
                facet
            );
      }
    }


     protected static void addMediaNestedRelationAggregation(
         @Nonnull String prefix,
         @Nonnull FilterAggregationBuilder rootAggregation,
         @Nonnull String nestedObject,
         @Nonnull String facetField,
         @Nullable RelationSearch allRelationSearch,
         @Nullable NameableSearchableLimitableFacet<MediaSearch, RelationSearch> facet) {

        // TODO use allRelationsSearch

        addNestedAggregation(
            prefix,
            rootAggregation,
            nestedObject,
            facetField,
            facet,
            (s) -> ESMediaFilterBuilder.filter(prefix, s),
            () -> ESMediaFilterBuilder.filterRelationsNested(prefix, allRelationSearch),
            (relationSearch, no, ff) -> ESMediaFilterBuilder.filterRelations(prefix, relationSearch),
            facet::getName
        );
     }

    protected static void addNestedTitlesAggregations(
        @Nonnull String prefix,
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nullable TitleFacetList facets) {
        if (facets == null || facets.isEmpty()) {
            return;
        }

        TitleSearch titleSearch = facets.getSubSearch();
        for (TitleFacet facet : facets) {
            QueryBuilder filter = facet.hasSubSearch() ?
                ESMediaQueryBuilder.filter(prefix, "expandedTitles", facet.getSubSearch()) : null;
            if (titleSearch != null) {
                if (filter == null) {
                    filter = ESMediaQueryBuilder.filter(prefix, "expandedTitles", titleSearch);
                } else {
                    ESMediaQueryBuilder.buildTitleQuery("", (BoolQueryBuilder) filter, titleSearch);
                }
            }
            if (filter == null) {
                throw new RuntimeException("No filter found for " + facet);
            }

            FilterAggregationBuilder filterAggregationBuilder =
                AggregationBuilders.filter(escape(facet.getName()), filter);
            rootAggregation.subAggregation(filterAggregationBuilder);
        }
    }

    protected static AggregationBuilder getFilteredRelationTermsBuilder(
        @Nonnull String pathPrefix,
        @Nonnull String nestedField,
        @Nonnull String facetField,
        @Nonnull RelationFacet facet,
        @Nonnull QueryBuilder subSearch
    ) {
        return getFilteredTermsBuilder(
            pathPrefix, nestedField, esExtendedTextField(facetField, facet.isCaseSensitive()),
            facet,
            facet::getName

        );
    }

    private static AggregationBuilder filterAggregation(
        @Nonnull String pathPrefix,
        @Nonnull String facetName,
        @Nonnull AggregationBuilder aggregationBuilder,
        @Nonnull MediaSearch... mediaSearch) {

        for (MediaSearch search : mediaSearch) {
            QueryBuilder query = ESMediaQueryBuilder.query(pathPrefix, search);
            aggregationBuilder = AggregationBuilders
                .filter(getFilterName(pathPrefix, facetName), query)
                .subAggregation(aggregationBuilder);
        }

        return aggregationBuilder;
    }


    protected static void addMediaFacet(
        @NotNull String prefix,
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nonnull String fieldName,
        @Nullable LimitableFacet<MediaSearch> facet) {
        ESFacetsBuilder.addFacet(
            prefix,
            rootAggregation,
            fieldName,
            facet,
            (boolQueryBuilder, mediaSearch) -> ESMediaQueryBuilder.buildMediaQuery(prefix, boolQueryBuilder, mediaSearch));

    }

}
