package nl.vpro.domain.api;

import org.elasticsearch.search.aggregations.BucketOrder;

/**
 * @author Michiel Meeuwissen
 * @since 3.3
 */
public class ESFacets {

    static BucketOrder getComparatorType(LimitableFacet<?> facet) {
        if (facet.getSort() == null) {
            return BucketOrder.count(false);
        }
        switch (facet.getSort()) {
            case VALUE_ASC:
                return BucketOrder.key(true);
            case VALUE_DESC:
                return BucketOrder.key(false);
            case COUNT_DESC:
                return BucketOrder.count(false);
            case COUNT_ASC:
            default:
                return BucketOrder.count(true);
        }
    }

    static BucketOrder getTermsOrder(LimitableFacet<?> facet) {
        if (facet.getSort() == null) {
            return BucketOrder.count(false);
        }
        switch (facet.getSort()) {
            case VALUE_ASC:
                return BucketOrder.key(true);
            case VALUE_DESC:
                return BucketOrder.key(false);
            case COUNT_ASC:
                return BucketOrder.count(true);
            case COUNT_DESC:
            default:
                return BucketOrder.count(false);
        }
    }
}
