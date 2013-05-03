package nl.vpro.api.service.search.filterbuilder;

public class PrefixFieldFilter extends AbstractFieldFilter {

    public PrefixFieldFilter(String field, String prefix) {
        super(field, prefix);
    }

    @Override
    public String createSolrQueryString() {
        return field + ":" + value + "*";
    }
}
