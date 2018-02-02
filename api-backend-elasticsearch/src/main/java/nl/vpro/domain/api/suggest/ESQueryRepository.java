package nl.vpro.domain.api.suggest;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.AbstractESRepository;
import nl.vpro.domain.api.SuggestResult;
import nl.vpro.domain.api.Suggestion;
import nl.vpro.domain.api.media.Redirector;
import nl.vpro.elasticsearch.ESClientFactory;
import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Roelof Jan Koekoek
 * @since 3.2
 */
public class ESQueryRepository extends AbstractESRepository<Query> implements QuerySearchRepository {
    private static final String[] RELEVANT_TYPES = new String[]{"query"};
    private static final Integer PROFILE_SEPARATOR_LENGTH = "||".length();


    @Override
    @Value("${elasticSearch.query.index}")
    public void setIndexName(String indexName) {
        super.setIndexName(indexName);
    }

    @Value("${elasticSearch.query.ttl}")
    private long queryTtl;

    @Autowired
    public ESQueryRepository(ESClientFactory client) {
        super(client);
    }

    @Override
    protected String[] getRelevantTypes() {
        return RELEVANT_TYPES;
    }

    @Override
    protected String[] getLoadTypes() {
        return RELEVANT_TYPES;

    }

    @Override
    protected Redirector getDirectsRepository() {
        throw new UnsupportedOperationException();

    }

    @Override
    public void index(Query query) {
        try {
            client().prepareIndex(getIndexName(), "query")
                //.setOpType(DocWriteRequest.)
                .setSource(Jackson2Mapper.getInstance().writeValueAsString(query), XContentType.JSON)
                .setTTL(queryTtl) // ms
                .setId(query.getId())
                .execute()
                .actionGet();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SuggestResult suggest(String input, String profile, Integer max) {
        CompletionSuggestionBuilder response = new CompletionSuggestionBuilder("completions");

        SearchResponse searchResponse = client().prepareSearch(getIndexName())
            //.suggest(suggestBuilder(Query.queryId(input, profile), profile, max).build(asdfkl))
            .execute()
            .actionGet();

        Suggest suggest = searchResponse.getSuggest();
        return adapt(suggest, input, profile);
    }

    private CompletionSuggestionBuilder suggestBuilder(String input, String profile, Integer max) {
        int profilePrefixLength = profile != null ? profile.length() + PROFILE_SEPARATOR_LENGTH : 0;

        // TODO
        return new CompletionSuggestionBuilder("suggest")
            .text(input)
            //.field("suggest")
            .size(max)
            //.setFuzziness(Fuzziness.AUTO)
            //.setFuzzyMinLength(profilePrefixLength + 3);
        ;
    }

    private Comparator<Suggestion> getLexicalDistanceComparator(final String input) {
        return Comparator.comparingInt(o -> StringUtils.getLevenshteinDistance(input, o.getText()));
    }

    SuggestResult adapt(Suggest suggestions, final String input, final String profile) {

        Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> esSuggestion = suggestions.getSuggestion("suggest");
        if (esSuggestion != null && esSuggestion.getEntries().size() > 0) {
            final List<? extends Suggest.Suggestion.Entry.Option> options = esSuggestion.getEntries().get(0).getOptions();
            if (options != null) {
                final List<Suggestion> suggestionsList = options.stream()
                    .map(option -> {
                        String text = option.getText().string();
                        return new Suggestion(profile != null ? text.substring(profile.length() + PROFILE_SEPARATOR_LENGTH) : text);
                    })
                    .sorted(getLexicalDistanceComparator(input))
                    .collect(Collectors.toList());

                return new SuggestResult(suggestionsList,
                    null,
                    options.size());
            }
        }

        return SuggestResult.emptyResult();
    }
}
