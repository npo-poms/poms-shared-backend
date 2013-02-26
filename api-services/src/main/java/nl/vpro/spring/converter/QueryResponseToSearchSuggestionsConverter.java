package nl.vpro.spring.converter;

import nl.vpro.api.transfer.SearchSuggestion;
import nl.vpro.api.transfer.SearchSuggestions;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.core.convert.converter.Converter;

/**
 * Date: 21-3-12
 * Time: 14:35
 *
 * @author Ernst Bunders
 */
public class QueryResponseToSearchSuggestionsConverter implements Converter<QueryResponse, SearchSuggestions> {
    @Override
    public SearchSuggestions convert(QueryResponse queryResponse) {
        SearchSuggestions suggestions = new SearchSuggestions();

        for (FacetField facetField: queryResponse.getFacetFields()) {
            if (facetField != null && facetField.getValues() != null) {
                for (FacetField.Count count : facetField.getValues()) {
                    if (suggestionPresent(suggestions, count.getName())) {
                        mergeSuggestion(suggestions, count);
                    } else {
                        suggestions.addSuggestion(new SearchSuggestion(count.getName(), count.getCount()));
                    }
                }
            }
        }
        return suggestions;
    }

    private void mergeSuggestion(SearchSuggestions suggestions, FacetField.Count count) {
        SearchSuggestion suggestion = findSuggestion(suggestions, count.getName());
        suggestion.setOccurrence(suggestion.getOccurrence() + count.getCount());
    }

    private boolean suggestionPresent(SearchSuggestions suggestions, String name) {
        return findSuggestion(suggestions, name) != null;
    }

    private SearchSuggestion findSuggestion(SearchSuggestions suggestions, String name) {
        for (SearchSuggestion suggestion : suggestions.getSuggestions()) {
            if (suggestion.getValue().equals(name)) {
                return suggestion;
            }
        }
        return null;
    }


}
