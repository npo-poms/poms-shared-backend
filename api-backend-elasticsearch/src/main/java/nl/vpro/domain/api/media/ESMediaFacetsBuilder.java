/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import nl.vpro.domain.api.*;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@Slf4j
public class ESMediaFacetsBuilder extends ESFacetsBuilder {

    protected static final String ROOT_FILTER = "mediaRootFilter";


    public static void facets(SearchSourceBuilder searchBuilder, MediaForm form) {
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
                ESMediaQueryBuilder.query(prefix, search, facetRootFilter);
            }

            // Append all aggregations to this filtered aggregation builder
            FilterAggregationBuilder rootAggregation = AggregationBuilders
                .filter(ROOT_FILTER, facetRootFilter);

            searchBuilder.aggregation(rootAggregation);

            {
                TitleFacetList titles = facets.getTitles();
                if (titles != null) {
                    if (titles.asMediaFacet()) {
                        addFacet(searchBuilder, prefix +  "titles.value.full", titles);
                    }
                    addNestedTitlesAggregations(rootAggregation, titles, prefix);
                }

            }

            {
                MediaFacet types = facets.getTypes();
                addFacet(searchBuilder, prefix + "type", types);
            }

            {
                MediaFacet avTypes = facets.getAvTypes();
                addFacet(searchBuilder, prefix + "avType", avTypes);

            }

            {
                DateRangeFacets sortDates = facets.getSortDates();
                addFacet(searchBuilder, prefix + "sortDate", sortDates);
            }

            {
                MediaFacet broadcasters = facets.getBroadcasters();
                //addFacetFilter(prefix, broadcasters, facetRootFilter);
                addFacet(searchBuilder, prefix + "broadcasters.id", broadcasters);

            }

            {
                MediaSearchableTermFacet genres = facets.getGenres();
                addNestedAggregation("genres", rootAggregation, "id", genres, prefix);
            }

            {
                ExtendedMediaFacet tags = facets.getTags();
                //addFacetFilter(prefix, tags, facetRootFilter);
                addFacet(searchBuilder, prefix + esField("tags", tags), tags);
            }

            {
                DurationRangeFacets durations = facets.getDurations();
                addFacet(searchBuilder, prefix + "duration", durations);
            }

            {
                MemberRefFacet descendantOf = facets.getDescendantOf();
                addNestedAggregation("descendantOf", rootAggregation, "midRef", descendantOf, prefix);
            }

            {
                MemberRefFacet episodeOf = facets.getEpisodeOf();
                addNestedAggregation("episodeOf", rootAggregation, "midRef", episodeOf, prefix);
            }

            {
                MemberRefFacet memberOf = facets.getMemberOf();
                addNestedAggregation("memberOf", rootAggregation, "midRef", memberOf, prefix);
            }

            {
                RelationFacetList relations = facets.getRelations();
                addNestedRelationAggregations(rootAggregation, "value", relations, prefix);
            }

            {
                MediaFacet ageRatings = facets.getAgeRatings();
                //addFacetFilter(prefix, ageRatings, facetRootFilter);
                addFacet(searchBuilder, prefix + "ageRating", ageRatings);
            }

            {
                MediaFacet contentRatings = facets.getContentRatings();
                //addFacetFilter(prefix, contentRatings, facetRootFilter);
                addFacet(searchBuilder, prefix + "contentRatings", contentRatings);

            }
        }
    }

    protected static void addNestedAggregation(
        @Nonnull String nestedField,
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nonnull String facetField,
        @Nullable MediaSearchableTermFacet facet,
        @Nonnull String pathPrefix) {
        if (facet == null) {
            return;
        }

        AggregationBuilder terms = getFilteredTermsBuilder(
            pathPrefix,
            nestedField,
            facetField,
            facet);

        //facet.hasSubSearch() ? ESFilterBuilder.filter(facet.getSubSearch(), pathPrefix, nestedField, facetField) : null

        NestedAggregationBuilder nestedBuilder = getNestedBuilder(
            pathPrefix,
            nestedField,
            null,
            terms
        );
        AggregationBuilder builder = filterAggregation(pathPrefix, nestedField, nestedBuilder, facet.getFilter());
        log.debug("Added aggregation {}", builder);
        rootAggregation.subAggregation(builder);

    }

    protected static void addNestedAggregation(
        @Nonnull String nestedField,
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nonnull String facetField,
        @Nullable MemberRefFacet facet,
        @Nonnull String pathPrefix) {
        if (facet == null) {
            return;
        }

        AggregationBuilder terms = getFilteredTermsBuilder(
            pathPrefix,
            nestedField,
            facetField,
            facet);
        //
        //facet.hasSubSearch() ? ESMediaFilterBuilder.filter(pathPrefix, nestedField, facet.getSubSearch()) : null

        NestedAggregationBuilder nestedBuilder = getNestedBuilder(
            pathPrefix,
            nestedField,
            null,
            terms
        );
        AggregationBuilder builder = filterAggregation(pathPrefix, nestedField, nestedBuilder, facet.getFilter());
        rootAggregation.subAggregation(builder);

    }

    protected static void addNestedRelationAggregations(
        @Nonnull FilterAggregationBuilder rootAggregation,
        @Nonnull String facetField,
        @Nullable RelationFacetList facets,
        @Nonnull String pathPrefix) {
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

            String facetName = escapeFacetName(facet.getName());
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
                AggregationBuilders.filter(escapeFacetName(facet.getName()), filter);
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
        return getFilteredTermsBuilder(pathPrefix, nestedField, esField(facetField, facet.isCaseSensitive()), facet);
    }

    private static AggregationBuilder filterAggregation(
        @Nonnull String pathPrefix,
        @Nonnull String facetName,
        @Nonnull AggregationBuilder aggregationBuilder,
        @Nonnull MediaSearch... mediaSearch) {

        for (MediaSearch search : mediaSearch) {
            BoolQueryBuilder facetFilter = QueryBuilders.boolQuery();
            ESMediaQueryBuilder.query(pathPrefix, search, facetFilter);
            aggregationBuilder = AggregationBuilders
                .filter(getFilterName(facetName), facetFilter)
                .subAggregation(aggregationBuilder);
        }

        return aggregationBuilder;
    }


    protected static void addFacet(
        @Nonnull  SearchSourceBuilder searchBuilder,
        @Nonnull  String fieldName,
        @Nullable TextFacet<MediaSearch> facet) {

        TermsAggregationBuilder aggregationBuilder = ESFacetsBuilder.createAggregationBuilder(fieldName, facet);
        if(aggregationBuilder != null) {
            MediaSearch facetFilter = facet.getFilter();
            if (facetFilter != null) {
                QueryBuilder query = ESMediaQueryBuilder.query(facetFilter);
                FilterAggregationBuilder filterAggregationBuilder =
                    AggregationBuilders.filter(fieldName + FACET_POSTFIX, query);
                filterAggregationBuilder.subAggregation(aggregationBuilder);
                searchBuilder.aggregation(filterAggregationBuilder);
            } else {
                searchBuilder.aggregation(aggregationBuilder);
            }
        }
    }

}
