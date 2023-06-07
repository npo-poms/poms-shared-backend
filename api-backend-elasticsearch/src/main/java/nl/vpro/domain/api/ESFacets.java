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
        return switch (facet.getSort()) {
            case VALUE_ASC -> BucketOrder.key(true);
            case VALUE_DESC -> BucketOrder.key(false);
            case COUNT_DESC -> BucketOrder.count(false);
            default -> BucketOrder.count(true);
        };
    }

    static BucketOrder getTermsOrder(LimitableFacet<?> facet) {
        if (facet.getSort() == null) {
            return BucketOrder.count(false);
        }
        return switch (facet.getSort()) {
            case VALUE_ASC -> BucketOrder.key(true);
            case VALUE_DESC -> BucketOrder.key(false);
            case COUNT_ASC -> BucketOrder.count(true);
            default -> BucketOrder.count(false);
        };
    }
}
