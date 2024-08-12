package nl.vpro.api.rs.filter;

/**
 * @author Michiel Meeuwissen
 * @since 4.9
 */
public class PropertyBuilder {

    public static PropertyBuilder builder() {
        return new PropertyBuilder();
    }

    private PropertyBuilder() {

    }

    public String build() {
        return "all";
    }
}
