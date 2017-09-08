package nl.vpro.domain.api;

import javax.annotation.Nonnull;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.constraint.*;
import nl.vpro.domain.constraint.media.HasLocationConstraint;
import nl.vpro.domain.constraint.media.HasPredictionConstraint;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public abstract class ESFilterBuilder {

    protected static void apply(BoolQueryBuilder answer, QueryBuilder filter, Match match) {
        switch (match) {
            case SHOULD:
                answer.should(filter);
                break;
            case MUST:
                answer.must(filter);
                break;
            case NOT:
                answer.mustNot(filter);
                break;
        }
    }

    public static <T> QueryBuilder filter(ProfileDefinition<T> definition) {
        if (isEmpty(definition)) {
            return matchAllQuery();
        }

        return handleConstraint(definition.getFilter().getConstraint());
    }

    public static <T> QueryBuilder filter(ProfileDefinition<T> definition, QueryBuilder  filter) {
        if (filter == null || filter instanceof MatchAllQueryBuilder) {
            return filter(definition);
        }

        if (isEmpty(definition)) {
            return filter;
        }

        // unwrap
        if (filter instanceof BoolQueryBuilder) {
            return ((BoolQueryBuilder) filter).must(filter(definition));
        } else {
            BoolQueryBuilder result = QueryBuilders.boolQuery();
            result.must(filter(definition));
            result.must(filter);
            return result;
        }
    }

    public static QueryBuilder filter(TermSearch searches, @Nonnull String axis, String field) {
        return filter(searches, "", axis, field);
    }

    public static QueryBuilder filter(TermSearch searches, @Nonnull String prefix, String axis, String field) {
        if(searches == null || searches.getIds() == null || searches.getIds().isEmpty()) {
            return matchAllQuery();
        }

        BoolQueryBuilder booleanFilter = boolQuery();
        build(booleanFilter, searches.getIds(), new SimpleFieldApplier(prefix + axis + '.' + field));
        return booleanFilter;
    }

    static <T> boolean isEmpty(ProfileDefinition<T> definition) {
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
                return QueryBuilders.termQuery(hasPrediction.getESPath(), hasPrediction.getPlatform());
            } else {
                return doExistsConstraint(hasPrediction);
            }
        } else if (constraint instanceof WildTextConstraint) {
            return doTextConstraint((WildTextConstraint<T>) constraint);
        } else if (constraint instanceof TextConstraint) {
            return doTextConstraint((TextConstraint<T>) constraint);
        } else if (constraint instanceof ExistsConstraint) {
            return doExistsConstraint((ExistsConstraint<T>) constraint);
        } else if (constraint instanceof  DateConstraint) {
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
        BoolQueryBuilder  booleanFilter = boolQuery();
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
        switch(constraint.getOperator()) {
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

    protected interface FieldApplier {
        <S extends MatchType> void applyField(BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcher<S> matcher);
    }


    protected static class SimpleFieldApplier implements FieldApplier {
        private final String fieldName;

        public SimpleFieldApplier(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public <S extends MatchType> void applyField(BoolQueryBuilder boolFilterBuilder, AbstractTextMatcher<S> matcher) {
            QueryBuilder filter = buildFilter(fieldName, matcher);
            apply(boolFilterBuilder, filter, matcher.getMatch());
        }
    }

    protected static class MultipleFieldApplier implements FieldApplier {
        private final String[] fieldNames;

        public MultipleFieldApplier(String... fieldNames) {
            this.fieldNames = fieldNames;
        }

        @Override
        public <S extends MatchType> void applyField(BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcher<S> matcher) {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            for (String fieldName : fieldNames) {
                QueryBuilder query = buildFilter(fieldName, matcher);
                apply(bool, query, Match.SHOULD);
            }
            apply(booleanQueryBuilder, bool, matcher.getMatch());
        }
    }

    protected static <S extends MatchType>  void build(BoolQueryBuilder booleanQueryBuilder, AbstractTextMatcherList<? extends AbstractTextMatcher<S>, S> list, FieldApplier applier) {
        BoolQueryBuilder append = QueryBuilders.boolQuery();
        for (AbstractTextMatcher<S> matcher : list) {
            applier.applyField(append, matcher);
        }
        apply(booleanQueryBuilder, append, list.getMatch());
    }

    protected static <S extends MatchType> QueryBuilder buildFilter(String fieldName, AbstractTextMatcher<S> matcher) {
        return buildFilter(fieldName, matcher, false);
    }

    protected static <S extends MatchType> QueryBuilder buildFilter(String fieldName, AbstractTextMatcher<S> matcher, boolean makeLowerCase) {
        String value = matcher.getValue();
        if (makeLowerCase) {
            value = value.toLowerCase();
        }
        ESMatchType matchType = ESMatchType.valueOf(matcher.getMatchType().getName());
        return matchType.getFilterBuilder(fieldName, value, matcher.isCaseSensitive());
    }

}
