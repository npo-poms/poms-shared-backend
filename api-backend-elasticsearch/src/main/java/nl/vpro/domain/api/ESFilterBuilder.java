package nl.vpro.domain.api;

import javax.annotation.Nonnull;

import org.elasticsearch.index.query.*;

import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.constraint.*;
import nl.vpro.domain.constraint.media.HasLocationConstraint;
import nl.vpro.domain.constraint.media.HasPredictionConstraint;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public abstract class ESFilterBuilder {

    protected static void apply(BoolFilterBuilder answer, FilterBuilder filter, Match match) {
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

    public static <T> FilterBuilder filter(ProfileDefinition<T> definition) {
        if (isEmpty(definition)) {
            return FilterBuilders.matchAllFilter();
        }

        return handleConstraint(definition.getFilter().getConstraint());
    }

    public static <T> FilterBuilder filter(ProfileDefinition<T> definition, FilterBuilder filter) {
        if (filter == null || filter instanceof MatchAllFilterBuilder) {
            return filter(definition);
        }

        if (isEmpty(definition)) {
            return filter;
        }

        // unwrap
        if (filter instanceof AndFilterBuilder) {
            return ((AndFilterBuilder) filter).add(filter(definition));
        } else if (filter instanceof BoolFilterBuilder) {
            return ((BoolFilterBuilder) filter).must(filter(definition));
        } else {
            return FilterBuilders.andFilter(filter(definition), filter);
        }
    }

    public static FilterBuilder filter(TermSearch searches, @Nonnull String axis, String field) {
        return filter(searches, "", axis, field);
    }

    public static FilterBuilder filter(TermSearch searches, @Nonnull String prefix, String axis, String field) {
        if(searches == null || searches.getIds() == null || searches.getIds().isEmpty()) {
            return FilterBuilders.matchAllFilter();
        }

        BoolFilterBuilder booleanFilter = FilterBuilders.boolFilter();
        build(booleanFilter, searches.getIds(), new SimpleFieldApplier(prefix + axis + '.' + field));
        return booleanFilter;
    }

    static <T> boolean isEmpty(ProfileDefinition<T> definition) {
        return definition == null || !definition.hasConstraint();
    }

    static private <T> FilterBuilder handleConstraint(Constraint<T> constraint) {
        if (constraint instanceof AbstractAnd) {
            return doAnd((AbstractAnd<T>) constraint);
        } else if (constraint instanceof AbstractOr) {
            return doOr((AbstractOr<T>) constraint);
        } else if (constraint instanceof AbstractNot) {
            return doNot((AbstractNot<T>) constraint);
        } else if (constraint instanceof HasLocationConstraint) {
            HasLocationConstraint hasLocation = (HasLocationConstraint) constraint;
            if (hasLocation.isNoPlatform()) {
                BoolFilterBuilder booleanFilter = FilterBuilders.boolFilter();
                booleanFilter.must(FilterBuilders.existsFilter("locations.urn"));
                booleanFilter.mustNot(FilterBuilders.existsFilter("locations.platform"));
                return FilterBuilders.nestedFilter("locations", booleanFilter);
            } else if (hasLocation.getPlatform() != null) {
                return FilterBuilders.termFilter(hasLocation.getESPath(), hasLocation.getPlatform());
            } else {
                return doExistsConstraint(hasLocation);
            }
        } else if (constraint instanceof HasPredictionConstraint) {
            HasPredictionConstraint hasPrediction = (HasPredictionConstraint) constraint;
            if (hasPrediction.getPlatform() != null) {
                return FilterBuilders.termFilter(hasPrediction.getESPath(), hasPrediction.getPlatform());
            } else {
                return doExistsConstraint(hasPrediction);
            }
        } else if (constraint instanceof TextConstraint) {
            return doTextConstraint((TextConstraint<T>) constraint);
        } else if (constraint instanceof ExistsConstraint) {
            return doExistsConstraint((ExistsConstraint<T>) constraint);
        } else if (constraint instanceof  DateConstraint) {
            return doDateConstraint((DateConstraint<T>) constraint);
        }

        throw new UnsupportedOperationException("No handling for " + constraint.getClass().getSimpleName());
    }

    static private <T> FilterBuilder doAnd(AbstractAnd<T> and) {
        BoolFilterBuilder booleanFilter = FilterBuilders.boolFilter();
        for (Constraint<T> constraint : and.getConstraints()) {
            FilterBuilder filter = handleConstraint(constraint);
            booleanFilter.must(filter);
        }
        return booleanFilter;
    }

    static private <T> FilterBuilder doOr(AbstractOr<T> or) {
        BoolFilterBuilder booleanFilter = FilterBuilders.boolFilter();
        for (Constraint<T> constraint : or.getConstraints()) {
            FilterBuilder filter = handleConstraint(constraint);
            booleanFilter.should(filter);
        }
        return booleanFilter;
    }

    static private <T> FilterBuilder doNot(AbstractNot<T> not) {
        BoolFilterBuilder booleanFilter = FilterBuilders.boolFilter();
        FilterBuilder filter = handleConstraint(not.getConstraint());
        booleanFilter.mustNot(filter);
        return booleanFilter;
    }

    static protected <T> FilterBuilder doTextConstraint(TextConstraint<T> constraint) {
        boolean exactMatch = constraint.isExact();
        String value = constraint.getValue();
        switch (constraint.getCaseHandling()) {
            case ASIS:
                return exactMatch ?
                        FilterBuilders.termFilter(constraint.getESPath(), value) :
                        FilterBuilders.queryFilter(QueryBuilders.wildcardQuery(constraint.getESPath(), '*' + value + '*'));
            case LOWER:
                return exactMatch ?
                        FilterBuilders.termFilter(constraint.getESPath(), value.toLowerCase()) :
                        FilterBuilders.queryFilter(QueryBuilders.wildcardQuery(constraint.getESPath(), '*' + value.toLowerCase() + '*'));
            case UPPER:
                return exactMatch ?
                        FilterBuilders.termFilter(constraint.getESPath(), value.toUpperCase()) :
                        FilterBuilders.queryFilter(QueryBuilders.wildcardQuery(constraint.getESPath(), '*' + value.toUpperCase() + '*'));
            default:
                return exactMatch ? new OrFilterBuilder(
                        FilterBuilders.termFilter(constraint.getESPath(), value.toLowerCase()),
                        FilterBuilders.termFilter(constraint.getESPath(), value.toUpperCase())
                ) : new OrFilterBuilder(
                        FilterBuilders.queryFilter(QueryBuilders.wildcardQuery(constraint.getESPath(), '*' + value.toLowerCase() + '*')),
                        FilterBuilders.queryFilter(QueryBuilders.wildcardQuery(constraint.getESPath(), value.toUpperCase()))
                );
        }
    }

    static protected <T> FilterBuilder doDateConstraint(DateConstraint<T> constraint) {
        switch(constraint.getOperator()) {
            case LT:
                return FilterBuilders.rangeFilter(constraint.getESPath()).lt(constraint.getDateAsDate().getTime());
            case LTE:
                return FilterBuilders.rangeFilter(constraint.getESPath()).lte(constraint.getDateAsDate().getTime());
            case GT:
                return FilterBuilders.rangeFilter(constraint.getESPath()).gt(constraint.getDateAsDate().getTime());
            case GTE:
                return FilterBuilders.rangeFilter(constraint.getESPath()).gte(constraint.getDateAsDate().getTime());
            default:
                throw new UnsupportedOperationException();
        }
    }

    static private <T> FilterBuilder doExistsConstraint(ExistsConstraint<T> constraint) {
        return FilterBuilders.existsFilter(constraint.getESPath());
    }

    protected interface FieldApplier {
        <S extends MatchType> void applyField(BoolFilterBuilder booleanQueryBuilder, AbstractTextMatcher<S> matcher);
    }


    protected static class SimpleFieldApplier implements FieldApplier {
        private final String fieldName;

        public SimpleFieldApplier(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public <S extends MatchType> void applyField(BoolFilterBuilder boolFilterBuilder, AbstractTextMatcher<S> matcher) {
            FilterBuilder filter = buildFilter(fieldName, matcher);
            apply(boolFilterBuilder, filter, matcher.getMatch());
        }
    }

    protected static class MultipleFieldApplier implements FieldApplier {
        private final String[] fieldNames;

        public MultipleFieldApplier(String... fieldNames) {
            this.fieldNames = fieldNames;
        }

        @Override
        public <S extends MatchType> void applyField(BoolFilterBuilder booleanQueryBuilder, AbstractTextMatcher<S> matcher) {
            BoolFilterBuilder bool = FilterBuilders.boolFilter();
            for (String fieldName : fieldNames) {
                FilterBuilder query = buildFilter(fieldName, matcher);
                apply(bool, query, Match.SHOULD);
            }
            apply(booleanQueryBuilder, bool, matcher.getMatch());
        }
    }

    protected static <S extends MatchType>  void build(BoolFilterBuilder booleanQueryBuilder, AbstractTextMatcherList<? extends AbstractTextMatcher<S>, S> list, FieldApplier applier) {
        BoolFilterBuilder append = FilterBuilders.boolFilter();
        for (AbstractTextMatcher<S> matcher : list) {
            applier.applyField(append, matcher);
        }
        apply(booleanQueryBuilder, append, list.getMatch());
    }

    protected static <S extends MatchType> FilterBuilder buildFilter(String fieldName, AbstractTextMatcher<S> matcher) {
        return buildFilter(fieldName, matcher, false);
    }

    protected static <S extends MatchType> FilterBuilder buildFilter(String fieldName, AbstractTextMatcher<S> matcher, boolean makeLowerCase) {
        String value = matcher.getValue();
        if (makeLowerCase) {
            value = value.toLowerCase();
        }
        ESMatchType matchType = ESMatchType.valueOf(matcher.getMatchType().getName());
        return matchType.getFilterBuilder(fieldName, value, matcher.isCaseSensitive());
    }

}
