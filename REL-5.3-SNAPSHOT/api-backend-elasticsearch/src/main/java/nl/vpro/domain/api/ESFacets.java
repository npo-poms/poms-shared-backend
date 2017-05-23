package nl.vpro.domain.api;

import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.facet.terms.TermsFacet;

/**
 * @author Michiel Meeuwissen
 * @since 3.3
 */
public class ESFacets {

    static TermsFacet.ComparatorType getComparatorType(TextFacet<?> facet) {
        if (facet.getSort() == null) {
            return TermsFacet.ComparatorType.COUNT;
        }
        switch (facet.getSort()) {
            case VALUE_ASC:
                return TermsFacet.ComparatorType.TERM;
            case VALUE_DESC:
                return TermsFacet.ComparatorType.REVERSE_TERM;
            case COUNT_DESC:
                return TermsFacet.ComparatorType.COUNT;
            case COUNT_ASC:
            default:
                return TermsFacet.ComparatorType.REVERSE_COUNT;
        }
    }

    static Terms.Order getTermsOrder(TextFacet<?> facet) {
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
