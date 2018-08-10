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


    public static void mediaFacets(
        @Nonnull SearchSourceBuilder searchBuilder,
        @Nullable MediaForm form) {
        buildMediaFacets("", searchBuilder, form);
    }

    /**
     * @param prefix empty string or path to this field including the last dot e.g., "embeds.media."
     */
    public static void buildMediaFacets(
        @Nonnull String prefix,
        @Nonnull SearchSourceBuilder searchBuilder,
        @Nullable MediaForm form) {
        if (form != null && form.isFaceted()) {

            MediaFacets facets = form.getFacets();
            BoolQueryBuilder facetRootFilter = QueryBuilders.boolQuery();

            // Under default ES behaviour profile filtering does not influence facet results
            if (facets.getFilter() != null) {
                MediaSearch search = facets.getFilter();
                QueryBuilder query = ESMediaQueryBuilder.query(prefix, search);
                // TODO?
            }

            // Append all aggregations to this filtered aggregation builder
            FilterAggregationBuilder rootAggregation = AggregationBuilders
                .filter(ROOT_FILTER, facetRootFilter);

            searchBuilder.aggregation(rootAggregation);

            {
                TitleFacetList titles = facets.getTitles();
                if (titles != null) {
                    if (titles.asMediaFacet()) {
                        addMediaFacet(prefix, rootAggregation, "titles.value.full", titles);
                    }
                    addNestedTitlesAggregations(rootAggregation, titles, prefix);
                }

            }

            addMediaFacet(prefix, rootAggregation, "type", facets.getTypes());

            addMediaFacet(prefix, rootAggregation, "avType", facets.getAvTypes());

            addDateRangeFacet(prefix, searchBuilder, "sortDate", facets.getSortDates());

            addMediaFacet(prefix, rootAggregation, "broadcasters.id", facets.getBroadcasters());

            addMediaNestedAggregation(prefix, "genres", "id", rootAggregation, facets.getGenres());

            {
                ExtendedMediaFacet tags = facets.getTags();
                //addFacetFilter(prefix, tags, facetRootFilter);
                addMediaFacet(prefix, rootAggregation, esExtendedTextField("tags", tags), tags);
            }
            addDurationFacet(prefix, searchBuilder, "duration", facets.getDurations());

            addNestedAggregationMemberRefSearch(prefix, "descendantOf", "midRef", rootAggregation, facets.getDescendantOf());

            addNestedAggregationMemberRefSearch(prefix, "episodeOf", "midRef", rootAggregation, facets.getEpisodeOf());

            addNestedAggregationMemberRefSearch(prefix, "memberOf", "midRef", rootAggregation, facets.getMemberOf());

            addNestedRelationAggregations(prefix, "value", rootAggregation, facets.getRelations());

            addMediaFacet(prefix, rootAggregation, "ageRating", facets.getAgeRatings());

            addMediaFacet(prefix, rootAggregation, "contentRatings",  facets.getContentRatings());

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
            mediaSearch -> ESMediaFilterBuilder.filter(pathPrefix, mediaSearch),
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

    protected static void addNestedRelationAggregations(
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
            addMediaNestedRelationAggregation(pathPrefix,"relations", esExtendedTextField("value", facet), rootAggregation, allRelationsFacetsSubSearch, facet);

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
     protected static void addMediaNestedRelationAggregation(
         @Nonnull String prefix,
         @Nonnull  String nestedObject,
         @Nonnull  String facetField,
         @Nonnull  FilterAggregationBuilder rootAggregation,
         @Nullable RelationSearch allRelationSearch,
         @Nullable NameableSearchableLimitableFacet<MediaSearch, RelationSearch> facet) {

        // TODO use allRelationsSearch

        addNestedAggregation(
            prefix,
            nestedObject,
            facetField,
            rootAggregation,
            facet,
            (s) -> ESMediaFilterBuilder.filter(prefix, s),
            (relationSearch, no, ff) -> ESMediaFilterBuilder.filterRelations(prefix, relationSearch),
            facet::getName
        );
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
