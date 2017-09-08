package nl.vpro.domain.api;

/**
 * @author Michiel Meeuwissen
 * @since 4.1
 */
public class SearchFieldDefinition {

    private final String name;

    private float boost;

    private final boolean highlight;

    public SearchFieldDefinition(String name, float boost, boolean highlight) {
        this.name = name;
        this.boost = boost;
        this.highlight = highlight;
    }

    public SearchFieldDefinition(String name, float boost) {
        this(name, boost, true);
    }

    public String getName() {
        return name;
    }

    public float getBoost() {
        return boost;
    }

    public void setBoost(float boost) {
        this.boost = boost;
    }

    public boolean isHighlight() {
        return highlight;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SearchFieldDefinition{");
        sb.append("name='").append(name).append('\'');
        sb.append(", boost=").append(boost);
        if(highlight) {
            sb.append(", highlighted");
        }
        sb.append('}');
        return sb.toString();
    }
}
