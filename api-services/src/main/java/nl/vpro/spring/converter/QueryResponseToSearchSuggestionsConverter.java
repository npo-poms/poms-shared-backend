package nl.vpro.spring.converter;

import nl.vpro.api.transfer.MediaSearchSuggestion;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.core.convert.converter.Converter;

import java.util.Arrays;

/**
 * Date: 21-3-12
 * Time: 14:35
 *
 * @author Ernst Bunders
 */
public class QueryResponseToSearchSuggestionsConverter implements Converter<QueryResponse, MediaSearchSuggestions> {
    @Override
    public MediaSearchSuggestions convert(QueryResponse queryResponse) {
        MediaSearchSuggestions suggestions = new MediaSearchSuggestions();

        for (String fieldName : Arrays.asList("titleMain", "descriptionMain")) {
            FacetField facetField = queryResponse.getFacetField(fieldName);
            if (facetField != null && facetField.getValues() != null) {
                for (FacetField.Count count : facetField.getValues()) {
                    if(suggestionPresent(suggestions, count.getName())) {
                        mergeSuggestion(suggestions, count);
                    }else{
                        suggestions.addSuggestion(new MediaSearchSuggestion(count.getName(), count.getCount()));
                    }
                }
            }
        }
        return suggestions;
    }

    private void mergeSuggestion(MediaSearchSuggestions suggestions, FacetField.Count count) {
        MediaSearchSuggestion suggestion =  findSuggestion(suggestions, count.getName());
        suggestion.setOccurrence(suggestion.getOccurrence() + count.getCount());
    }

    private boolean suggestionPresent(MediaSearchSuggestions suggestions, String name) {
        return findSuggestion(suggestions, name) != null;
    }

    private MediaSearchSuggestion findSuggestion(MediaSearchSuggestions suggestions,  String name) {
        for (MediaSearchSuggestion suggestion : suggestions.getSuggestions()) {
            if (suggestion.getValue().equals(name)) {
                return suggestion;
            }
        }
        return null;
    }


}
