package nl.vpro.domain.api.suggest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.common.text.StringText;
import org.elasticsearch.search.suggest.Suggest;
import org.junit.Test;

import nl.vpro.domain.api.SuggestResult;
import nl.vpro.domain.api.Suggestion;
import nl.vpro.elasticsearch.ESClientFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
public class ESQueryRepositoryTest {
    //https://jira.vpro.nl/browse/NPA-384
    @Test()
    public void adapt() throws Exception {
        ESClientFactory factory = mock(ESClientFactory.class);
        ESQueryRepository instance = new ESQueryRepository(factory);


        List<Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>>> suggestionList = new ArrayList<>();
        Suggest.Suggestion<Suggest.Suggestion.Entry<?>> suggestEntry = new Suggest.Suggestion<>("suggest", 5);
        suggestionList.add(suggestEntry);


        Suggest.Suggestion.Entry suggestion = new Suggest.Suggestion.Entry(new StringText("suggestion"), 0, 10);
        suggestEntry.addTerm(suggestion);

        suggestion.addOption(new Suggest.Suggestion.Entry.Option(new StringText("cuba"), 0.5f));
        suggestion.addOption(new Suggest.Suggestion.Entry.Option(new StringText("dubai"), 0.5f));
        suggestion.addOption(new Suggest.Suggestion.Entry.Option(new StringText("libanon"), 0.5f));
        suggestion.addOption(new Suggest.Suggestion.Entry.Option(new StringText("libanon andere tijden"), 0.5f));
        suggestion.addOption(new Suggest.Suggestion.Entry.Option(new StringText("lubach"), 0.5f));
        org.elasticsearch.search.suggest.Suggest suggest = new org.elasticsearch.search.suggest.Suggest(suggestionList);

        SuggestResult adapt = instance.adapt(suggest, "liba", null);
        assertThat(adapt.getItems()).hasSize(5);
        assertThat(adapt.getItems().stream().map(Suggestion::getText).collect(Collectors.toList()))
            .containsExactly("cuba", "dubai", "libanon", "lubach", "libanon andere tijden");
    }

}
