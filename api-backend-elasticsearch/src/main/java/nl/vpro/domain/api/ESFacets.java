package nl.vpro.domain.api;

import org.elasticsearch.search.aggregations.bucket.terms.Terms;

/**
 * @author Michiel Meeuwissen
 * @since 3.3
 */
public class ESFacets {

    static Terms.Order getComparatorType(LimitableFacet<?> facet) {
        if (facet.getSort() == null) {
            return Terms.Order.count(false);
        }
        switch (facet.getSort()) {
            case VALUE_ASC:
                return Terms.Order.term(true);
            case VALUE_DESC:
                return Terms.Order.term(false);
            case COUNT_DESC:
                return Terms.Order.count(false);
            case COUNT_ASC:
            default:
                return Terms.Order.count(true);
        }
    }

    static Terms.Order getTermsOrder(LimitableFacet<?> facet) {
        if (facet.getSort() == null) {
            return Terms.Order.count(false);
        }
        switch (facet.getSort()) {
            case VALUE_ASC:
                return Terms.Order.term(true);
            case VALUE_DESC:
                return Terms.Order.term(false);
            case COUNT_ASC:
                return Terms.Order.count(true);
            case COUNT_DESC:
            default:
                return Terms.Order.count(false);
        }
    }
}
