package nl.vpro.api.service.search.fiterbuilder;

/**
 * MM: See comments in {@link DocumentSearchFilter}
 * Date: 19-3-12
 * Time: 13:51
 *
 * @author Ernst Bunders
 */
public abstract class SearchFilter<T extends SearchFilter> {

    private BooleanOp booleanOp;

    protected String queryString;

    protected SearchFilter(BooleanOp booleanOp) {
        this.booleanOp = booleanOp;
    }

    public T setQueryString(String queryString) {
        this.queryString = queryString;
        return getInstance();
    }

    protected String wrapInQuotes(String s) {
        return "\"" + s + "\"";
    }

    public abstract String createQueryString();

    protected abstract T getInstance();

    public abstract boolean evaluate(Object object);


    @Override
    public String toString() {
        return createQueryString();
    }

    protected class BooleanGroupingStringBuilder {

        final StringBuilder stringBuilder = new StringBuilder();

        private boolean first = true;
        private boolean hasOpened = false;
        protected boolean grouping = true; /*should this group be wrapped in parenthesis?*/


        protected BooleanGroupingStringBuilder() {
        }

        public BooleanGroupingStringBuilder append(Object o) {
            if (first) {
                if (grouping) {
                    stringBuilder.append("(");
                    hasOpened = true;
                }
                first = false;
            } else {
                stringBuilder.append(" ").append(booleanOp.name()).append(" ");
            }
            stringBuilder.append(o);
            return this;
        }

        public void close() {
            if (grouping && hasOpened) {
                stringBuilder.append(")");
            }
            stringBuilder.append(" ");
            first = true;
        }

        @Override
        public String toString() {
            return stringBuilder.toString().trim();
        }

    }

    public BooleanOp getBooleanOp() {
        return booleanOp;
    }
}
