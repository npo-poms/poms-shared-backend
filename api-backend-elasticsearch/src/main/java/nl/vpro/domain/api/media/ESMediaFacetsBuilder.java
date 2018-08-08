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
import org.elasticsearch.index.query.QueryBuilders;
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


    public static void facets(
        @Nonnull SearchSourceBuilder searchBuilder,
        @Nullable MediaForm form) {
        buildFacets("", searchBuilder, form);
    }

    /**
     * @param prefix empty string of path to this field including the last dot e.g., "embeds.media."
     */
    public static void buildFacets(
        @Nonnull String prefix,
        @Nonnull SearchSourceBuilder searchBuilder,
        @Nullable MediaForm form) {
        if (form != null && form.isFaceted()) {

            MediaFacets facets = form.getFacets();
            BoolQueryBuilder facetRootFilter = QueryBuilders.boolQuery();

            // Under default ES behaviour profile filtering does not influence facet results
            if (facets.getFilter() != null) {
                MediaSearch search = facets.getFilter();
                //ESMediaQueryBuilder.query(prefix, search, facetRootFilter);
            }

            // Append all aggregations to this filtered aggregation builder
            FilterAggregationBuilder rootAggregation = AggregationBuilders
                .filter(ROOT_FILTER, facetRootFilter);

            searchBuilder.aggregation(rootAggregation);

            {
                TitleFacetList titles = facets.getTitles();
                if (titles != null) {
                    if (titles.asMediaFacet()) {
                        addMediaFacet(prefix, searchBuilder, "titles.value.full", titles);
                    }
                    addNestedTitlesAggregations(rootAggregation, titles, prefix);
                }

            }

            addMediaFacet(prefix, searchBuilder, "type", facets.getTypes());

            addMediaFacet(prefix, searchBuilder, "avType", facets.getAvTypes());

            addDateRangeFacet(prefix, searchBuilder, "sortDate", facets.getSortDates());

            addMediaFacet(prefix, searchBuilder, "broadcasters.id", facets.getBroadcasters());

            addMediaNestedAggregation(prefix, "genres", "id", rootAggregation, facets.getGenres());

            {
                ExtendedMediaFacet tags = facets.getTags();
                //addFacetFilter(prefix, tags, facetRootFilter);
                addMediaFacet(prefix, searchBuilder, esExtendedTextField("tags", tags), tags);
            }
            addDurationFacet(prefix, searchBuilder, "duration", facets.getDurations());

            addNestedAggregationMemberRefSearch(prefix, "descendantOf", "midRef", rootAggregation, facets.getDescendantOf());


            addNestedAggregationMemberRefSearch(prefix, "episodeOf", "midRef", rootAggregation, facets.getEpisodeOf());

            addNestedAggregationMemberRefSearch(prefix, "memberOf", "midRef", rootAggregation, facets.getMemberOf());

            addNestedRelationAggregationsRelationSearch(prefix, "value", rootAggregation, facets.getRelations());

            addMediaFacet(prefix, searchBuilder, "ageRating", facets.getAgeRatings());

            addMediaFacet(prefix, searchBuilder, "contentRatings",  facets.getContentRatings());

        }
    }

    protected static <S> void addMediaNestedAggregation(
        @Nonnull String pathPrefix,
        @Nonnull String nestedObject,
        @Nonnull String facetField,
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nullable SearchableLimitableFacet<MediaSearch, TermSearch> facet) {
        addNestedAggregation(
            pathPrefix,
            nestedObject,
            facetField,
            rootAggregation,
            facet,
            mediaSearch -> ESMediaFilterBuilder.filter(pathPrefix, facet.getFilter()),
            ESMediaQueryBuilder::search
        );

    }

    protected static void addNestedAggregationMemberRefSearch(
        @Nonnull String pathPrefix,
        @Nonnull String nestedObject,
        @Nonnull String facetField,
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nullable  SearchableLimitableFacet<MediaSearch, MemberRefSearch>  facet) {
        addNestedAggregation(
            pathPrefix,
            nestedObject,
            facetField,
            rootAggregation,
            facet,
            mediaSearch -> ESMediaFilterBuilder.filter(pathPrefix, mediaSearch),
            (memberRefSearch, nestedObject1, facetField1) -> ESMediaQueryBuilder.filter(memberRefSearch, nestedObject1)

        );

    }

    protected static void addNestedRelationAggregationsRelationSearch(
        @Nonnull String pathPrefix,
        @Nonnull String facetField,
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nullable RelationFacetList facets) {
        if (facets == null || facets.isEmpty()) {
            return;
        }

        // for all relation search
        RelationSearch allRelationsFacetsSubSearch = facets.getSubSearch();

        for (RelationFacet facet : facets) {
            BoolQueryBuilder facetFilter = QueryBuilders.boolQuery();
            if (facet.hasSubSearch()) {
                ESQueryBuilder.relationQuery(pathPrefix, facet.getSubSearch(), facetFilter);
            }
            if (allRelationsFacetsSubSearch != null) {
                ESQueryBuilder.relationQuery(pathPrefix, allRelationsFacetsSubSearch,  facetFilter);
            }
            AggregationBuilder termsBuilder = getFilteredRelationTermsBuilder(
                pathPrefix,
                "relations",
                facetField,
                facet,
                facetFilter
            );

            String facetName = escape(facet.getName());
            AggregationBuilder builder = filterAggregation(
                pathPrefix,
                facetName,
                termsBuilder,
                facet.getFilter(),
                facets.getFilter()
            );
            log.debug("Added aggregation {}", builder);
            rootAggregation.subAggregation(builder);
        }
    }

    protected static void addNestedTitlesAggregations(
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nullable TitleFacetList facets,
        @Nonnull String pathPrefix) {
        if (facets == null || facets.isEmpty()) {
            return;
        }

        TitleSearch titleSearch = facets.getSubSearch();
        for (TitleFacet facet : facets) {
            QueryBuilder filter = facet.hasSubSearch() ?
                ESMediaQueryBuilder.filter(pathPrefix, "expandedTitles", facet.getSubSearch()) : null;
            if (titleSearch != null) {
                if (filter == null) {
                    filter = ESMediaQueryBuilder.filter(pathPrefix, "expandedTitles", titleSearch);
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
        return getFilteredTermsBuilder(pathPrefix, nestedField, esExtendedTextField(facetField, facet.isCaseSensitive()), facet);
    }

    private static AggregationBuilder filterAggregation(
        @Nonnull String pathPrefix,
        @Nonnull String facetName,
        @Nonnull AggregationBuilder aggregationBuilder,
        @Nonnull MediaSearch... mediaSearch) {

        for (MediaSearch search : mediaSearch) {
            BoolQueryBuilder facetFilter = QueryBuilders.boolQuery();
            //ESMediaQueryBuilder.query(pathPrefix, search, facetFilter);
            aggregationBuilder = AggregationBuilders
                .filter(getFilterName(pathPrefix, facetName), facetFilter)
                .subAggregation(aggregationBuilder);
        }

        return aggregationBuilder;
    }


    protected static void addMediaFacet(
        @NotNull String prefix,
        @Nonnull SearchSourceBuilder searchBuilder,
        @Nonnull String fieldName,
        @Nullable LimitableFacet<MediaSearch> facet) {
        ESFacetsBuilder.addFacet(
            prefix,
            searchBuilder,
            fieldName,
            facet,
            (boolQueryBuilder, mediaSearch) -> ESMediaQueryBuilder.buildMediaQuery(prefix, boolQueryBuilder, mediaSearch));

    }

}
