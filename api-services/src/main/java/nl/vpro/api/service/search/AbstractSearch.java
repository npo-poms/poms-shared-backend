package nl.vpro.api.service.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ernst
 * Date: 9/26/12
 * Time: 4:41 PM
 *
 */
public abstract class AbstractSearch implements Search {
    private List<String> searchFields = Arrays.asList("titleMain", "titleAlternative", "descriptionMain", "descriptionShort", "descriptionAlternative");
    private List<Float> searchFieldBoosting = Arrays.asList(2.0f, 2.0f, 1.0f, 1.0f, 1.0f);

    public List<String> getSearchFields() {
        return Collections.unmodifiableList(searchFields);
    }

    public void setSearchFields(List<String> searchFields) {
        this.searchFields = searchFields;
    }

    public List<Float> getSearchFieldBoosting() {
        return Collections.unmodifiableList(searchFieldBoosting);
    }

    public void setSearchFieldBoosting(List<Float> searchFieldBoosting) {
        this.searchFieldBoosting = searchFieldBoosting;
    }
}
