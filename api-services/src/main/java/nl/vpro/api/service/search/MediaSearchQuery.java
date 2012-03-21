package nl.vpro.api.service.search;

/**
 * Date: 19-3-12
 * Time: 13:51
 *
 * @author Ernst Bunders
 */
public abstract class MediaSearchQuery<T extends MediaSearchQuery> {

    protected String queryString;

    public T setQueryString(String queryString) {
        this.queryString = queryString;
        return getInstance();
    }

    protected String wrapInQuotes(String s) {
        return "\"" + s + "\"";
    }

    public abstract String createQueryString();

    protected abstract T getInstance();


    protected static class BooleanGroupingStringBuilder {
        private boolean first = true;
        private boolean hasOpened = false;
        private boolean grouping = true;
        StringBuilder stringBuilder = new StringBuilder();
        private String operator;

        static BooleanGroupingStringBuilder ANDBuilder() {
            BooleanGroupingStringBuilder builder = new BooleanGroupingStringBuilder();
            builder.operator = "AND";
            return builder;
        }

        static BooleanGroupingStringBuilder ORBuilder() {
            BooleanGroupingStringBuilder builder = new BooleanGroupingStringBuilder();
            builder.operator = "OR";
            return builder;
        }

        private BooleanGroupingStringBuilder() {
        }

        public BooleanGroupingStringBuilder append(Object o) {
            if (first) {
                if (grouping) {
                    stringBuilder.append("(");
                    hasOpened = true;
                }
                first = false;
            } else {
                stringBuilder.append(" " + operator + " ");
            }
            stringBuilder.append(o);
            return this;
        }

        public void close() {
            if (grouping && hasOpened) {
                stringBuilder.append(") ");
            }
            first = true;
        }

        public String toString() {
            return stringBuilder.toString().trim();
        }

    }
}
