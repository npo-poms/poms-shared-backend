package nl.vpro.api.service.search.filterbuilder;

public abstract class AbstractFieldFilter extends SearchFilter {

    protected String field;
    protected String value;

    public AbstractFieldFilter(String field, String value) {
        super(BooleanOp.AND);
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
