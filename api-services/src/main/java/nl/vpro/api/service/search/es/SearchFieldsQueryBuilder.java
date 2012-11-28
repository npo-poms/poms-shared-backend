package nl.vpro.api.service.search.es;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

import java.util.List;

/**
 * Creates a BoolQueryBuilder for all the searchFields
 * User: ernst
 * Date: 10/2/12
 * Time: 12:55 PM
 */
public class SearchFieldsQueryBuilder extends BoolQueryBuilder {

    /**
     * @param fields the document fields to search on
     * @param boosting the boosting to apply to each fields (the order is maintained)
     * @param term the search term
     * @throws IllegalStateException when collections 'fields' en 'boosting' are not of equal size.
     */
    public SearchFieldsQueryBuilder(List<String> fields, List<Float> boosting, String term) throws IllegalStateException{
        if (fields.size() != boosting.size()) {
            throw new IllegalStateException("Lists 'fields' and 'boosting' should be of equal size");
        }

        for (int i = 0; i < fields.size(); i++) {
            should(new TermQueryBuilder(fields.get(i), term)
                    .boost(boosting.get(i)));
        }
    }
}
