package nl.vpro.poms.es;


import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static nl.vpro.poms.es.Utils.resourceToString;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
@lombok.Getter()
public abstract class AbstractIndex {

    private final String indexName;
    private final String mappingResource;
    private final String settingsResource;

    protected AbstractIndex(String indexName, String mappingResource) {
        this.indexName = indexName;
        this.settingsResource = "/es7/setting/" + indexName + ".json";
        this.mappingResource = mappingResource;
    }

    public Supplier<String> settings() {
        return () -> resourceToString(settingsResource);
    }

    public Map<String, Supplier<String>> mappingsAsMap() {
        Map<String, Supplier<String>> result = new HashMap<>();
        result.put("_doc", () -> resourceToString(mappingResource));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() != o.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
