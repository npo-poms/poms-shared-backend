package nl.vpro.api.service.searchfilterbuilder;

/**
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


    protected class BooleanGroupingStringBuilder {
        private boolean first = true;
        private boolean hasOpened = false;
        protected boolean grouping = true; /*should this group be wrapped in parenthesis?*/
        StringBuilder stringBuilder = new StringBuilder();

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

        public String toString() {
            return stringBuilder.toString().trim();
        }

    }

    public BooleanOp getBooleanOp() {
        return booleanOp;
    }
}
