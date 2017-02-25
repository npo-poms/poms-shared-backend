package nl.vpro.domain.api.suggest;

import java.util.AbstractList;
import java.util.List;

import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionFuzzyBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.AbstractESRepository;
import nl.vpro.domain.api.SuggestResult;
import nl.vpro.domain.api.Suggestion;
import nl.vpro.domain.api.media.MediaRepository;
import nl.vpro.elasticsearch.ESClientFactory;
import nl.vpro.jackson2.Jackson2Mapper;

/**
 * @author Roelof Jan Koekoek
 * @since 3.2
 */
public class ESQueryRepository extends AbstractESRepository<Query> implements QuerySearchRepository {
    private static final String[] RELEVANT_TYPES = new String[]{"query"};


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
    protected MediaRepository getDirectsRepository() {
        throw new UnsupportedOperationException();

    }

    @Override
    public void index(Query query) {
        try {
            client().prepareIndex(getIndexName(), "query")
                .setOperationThreaded(true)
                .setSource(Jackson2Mapper.getInstance().writeValueAsString(query))
                .setTTL(queryTtl) // ms
                .execute()
                .actionGet();
        } catch(JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SuggestResult suggest(String input, String profile, Integer max) {
        SuggestResponse response = new SuggestRequestBuilder(client())
            .setIndices(getIndexName())
            .addSuggestion(suggestBuilder(Query.queryId(input, profile), profile, max))
            .execute()
            .actionGet();

        Suggest suggest = response.getSuggest();
        return adapt(suggest, profile);
    }

    private SuggestBuilder.SuggestionBuilder suggestBuilder(String input, String profile, Integer max) {
        return new CompletionSuggestionFuzzyBuilder("suggest")
            .text(input)
            .field("suggest")
            .size(max)
            .setFuzziness(Fuzziness.AUTO)
            .setFuzzyMinLength(profile != null ? profile.length() + 3 : 3);
    }

    private SuggestResult adapt(Suggest suggestions, final String profile) {

        Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> esSuggestion = suggestions.getSuggestion("suggest");
        if(esSuggestion != null && esSuggestion.getEntries().size() > 0) {
            final List<? extends Suggest.Suggestion.Entry.Option> options = esSuggestion.getEntries().get(0).getOptions();
            if(options != null) {
                return new SuggestResult(new AbstractList<Suggestion>() {
                    @Override
                    public Suggestion get(int index) {
                        String text = options.get(index).getText().string();
                        return new Suggestion(profile != null ? text.substring(profile.length() + 2) : text);
                    }

                    @Override
                    public int size() {
                        return options.size();
                    }
                },
                null,
                options.size());
            }
        }

        return SuggestResult.emptyResult();
    }
}
