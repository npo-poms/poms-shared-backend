package nl.vpro.domain.api;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Michiel Meeuwissen
 * @since 4.1
 */
@Getter
@Setter
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


    public boolean isActive() {
        return boost > 0;
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
