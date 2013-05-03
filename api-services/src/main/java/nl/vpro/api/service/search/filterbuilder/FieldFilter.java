package nl.vpro.api.service.search.filterbuilder;

/**
 * Filter that checks whether a field contains a certain value.
 */
public class FieldFilter extends AbstractFieldFilter {

    public FieldFilter(String field, String value) {
        super(field, value);
    }

    @Override
    public String createSolrQueryString() {
        return field + ":" + value;
    }
}
