package nl.vpro.domain.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.constraint.*;
import nl.vpro.domain.constraint.media.HasLocationConstraint;
import nl.vpro.domain.constraint.media.HasPredictionConstraint;
import nl.vpro.domain.media.support.Workflow;

import static nl.vpro.domain.api.ESQueryBuilder.simplifyQuery;
import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public abstract class ESFilterBuilder {


    public static <T> QueryBuilder filter(ProfileDefinition<T> definition) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        filter(definition, boolQueryBuilder);
        return simplifyQuery(boolQueryBuilder);
    }

    /**
     * Handle profile and workflow filtering
     *
     */
    public static <T> void filter(
        @Nullable ProfileDefinition<T> definition,
        @NonNull BoolQueryBuilder rootQuery) {
        rootQuery.filter(QueryBuilders.termQuery("expandedWorkflow", Workflow.PUBLISHED.name()));
        if (!isEmpty(definition)) {
            rootQuery.filter(
                handleConstraint(definition.getFilter().getConstraint())
            );
        }
    }



    static <T> boolean isEmpty(
        @Nullable ProfileDefinition<T> definition) {
        return definition == null || !definition.hasConstraint();
    }

    static private <T> QueryBuilder handleConstraint(Constraint<T> constraint) {
        if (constraint instanceof AbstractAnd) {
            return doAnd((AbstractAnd<T>) constraint);
        } else if (constraint instanceof AbstractOr) {
            return doOr((AbstractOr<T>) constraint);
        } else if (constraint instanceof AbstractNot) {
            return doNot((AbstractNot<T>) constraint);
        } else if (constraint instanceof HasLocationConstraint) {
            HasLocationConstraint hasLocation = (HasLocationConstraint) constraint;
            if (hasLocation.isNoPlatform()) {
                BoolQueryBuilder booleanFilter = QueryBuilders.boolQuery();
                booleanFilter.must(QueryBuilders.existsQuery("locations.urn"));
                booleanFilter.mustNot(QueryBuilders.existsQuery("locations.platform"));
                return QueryBuilders.nestedQuery("locations", booleanFilter, ScoreMode.Avg);
            } else if (hasLocation.getPlatform() != null) {
                return QueryBuilders.termQuery(hasLocation.getESPath(), hasLocation.getPlatform());
            } else {
                return doExistsConstraint(hasLocation);
            }
        } else if (constraint instanceof HasPredictionConstraint) {
            HasPredictionConstraint hasPrediction = (HasPredictionConstraint) constraint;
            if (hasPrediction.getPlatform() != null) {
                return QueryBuilders.termQuery(hasPrediction.getESPath(), hasPrediction.getPlatform().name());
            } else {
                return doExistsConstraint(hasPrediction);
            }
        } else if (constraint instanceof WildTextConstraint) {
            return doTextConstraint((WildTextConstraint<T>) constraint);
        } else if (constraint instanceof TextConstraint) {
            return doTextConstraint((TextConstraint<T>) constraint);
        } else if (constraint instanceof ExistsConstraint) {
            return doExistsConstraint((ExistsConstraint<T>) constraint);
        } else if (constraint instanceof DateConstraint) {
            return doDateConstraint((DateConstraint<T>) constraint);
        }

        throw new UnsupportedOperationException("No handling for " + constraint.getClass().getSimpleName());
    }

    static private <T> QueryBuilder doAnd(AbstractAnd<T> and) {
        BoolQueryBuilder booleanFilter = boolQuery();
        for (Constraint<T> constraint : and.getConstraints()) {
            QueryBuilder filter = handleConstraint(constraint);
            booleanFilter.must(filter);
        }
        return booleanFilter;
    }

    static private <T> QueryBuilder doOr(AbstractOr<T> or) {
        BoolQueryBuilder booleanFilter = boolQuery();
        for (Constraint<T> constraint : or.getConstraints()) {
            QueryBuilder filter = handleConstraint(constraint);
            booleanFilter.should(filter);
        }
        return booleanFilter;
    }

    static private <T> QueryBuilder doNot(AbstractNot<T> not) {
        BoolQueryBuilder booleanFilter = boolQuery();
        QueryBuilder filter = handleConstraint(not.getConstraint());
        booleanFilter.mustNot(filter);
        return booleanFilter;
    }

    static protected <T> QueryBuilder doTextConstraint(WildTextConstraint<T> constraint) {
        boolean exactMatch = constraint.isExact();
        String value = exactMatch ? constraint.getValue() : constraint.getWildcardValue();
        switch (constraint.getCaseHandling()) {
            case ASIS:
                return exactMatch ?
                    termQuery(constraint.getESPath(), value) :
                    toWildCard(constraint.getESPath(), value);
            case LOWER:
                return exactMatch ?
                    termQuery(constraint.getESPath(), value.toLowerCase()) :
                    toWildCard(constraint.getESPath(), value.toLowerCase());
            case UPPER:
                return exactMatch ?
                    QueryBuilders.termQuery(constraint.getESPath(), value.toUpperCase()) :
                    toWildCard(constraint.getESPath(), value.toUpperCase());
            default:
                BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
                if (exactMatch) {
                    queryBuilder.should(QueryBuilders.termQuery(constraint.getESPath(), value.toLowerCase()));
                    queryBuilder.should(QueryBuilders.termQuery(constraint.getESPath(), value.toUpperCase()));

                } else {
                    queryBuilder.should(toWildCard(constraint.getESPath(), value.toLowerCase()));
                    queryBuilder.should(toWildCard(constraint.getESPath(), value.toUpperCase()));
                }
                return queryBuilder;
        }
    }

    static protected <T> QueryBuilder doTextConstraint(TextConstraint<T> constraint) {
        return termQuery(constraint.getESPath(), constraint.getValue());
    }

    static QueryBuilder toWildCard(String path, String value) {
        if (value.contains("*")) {
            return wildcardQuery(path, value);
        } else {
            return prefixQuery(path, value);

        }
    }

    static protected <T> QueryBuilder doDateConstraint(DateConstraint<T> constraint) {
        switch (constraint.getOperator()) {
            case LT:
                return rangeQuery(constraint.getESPath()).lt(constraint.getDateAsDate().getTime());
            case LTE:
                return rangeQuery(constraint.getESPath()).lte(constraint.getDateAsDate().getTime());
            case GT:
                return rangeQuery(constraint.getESPath()).gt(constraint.getDateAsDate().getTime());
            case GTE:
                return rangeQuery(constraint.getESPath()).gte(constraint.getDateAsDate().getTime());
            default:
                throw new UnsupportedOperationException();
        }
    }

    static private <T> QueryBuilder doExistsConstraint(ExistsConstraint<T> constraint) {
        return existsQuery(constraint.getESPath());
    }


}
