package nl.vpro.domain.api.suggest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.text.Text;
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
        testAdapt("luba",
            "cuba",
            "dubai",
            "libanon",
            "libanon andere tijden",
            "lubach"
        );
    }

    public void testAdapt(String input, String... suggestions) throws Exception {
        ESClientFactory factory = mock(ESClientFactory.class);
        ESQueryRepository instance = new ESQueryRepository(factory);
        org.elasticsearch.search.suggest.Suggest suggest = getSuggestResult(suggestions);

        SuggestResult adapt = instance.adapt(suggest, input, null);
        assertThat(adapt.getItems()).hasSameSizeAs(suggestions);
        int prevDistance = Integer.MIN_VALUE;
        for (Suggestion sug : adapt.getItems()) {
            int distance = StringUtils.getLevenshteinDistance(input, sug.getText());
            System.out.println(distance + ":" + sug.getText());
            assertThat(distance).isGreaterThanOrEqualTo(prevDistance);
            prevDistance = distance;
        }

    }

    private Suggest getSuggestResult(String... text) {

        List<Suggest.Suggestion<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>>> suggestionList = new ArrayList<>();
        Suggest.Suggestion<Suggest.Suggestion.Entry<?>> suggestEntry = new Suggest.Suggestion<>("suggest", 5);
        suggestionList.add(suggestEntry);


        Suggest.Suggestion.Entry<Suggest.Suggestion.Entry.Option> suggestion = new Suggest.Suggestion.Entry<>(new Text("suggestion"), 0, 10);
        suggestEntry.addTerm(suggestion);

        addOptions(suggestion, text);

        org.elasticsearch.search.suggest.Suggest suggest = new org.elasticsearch.search.suggest.Suggest(suggestionList);
        return suggest;

    }

    private void addOptions(Suggest.Suggestion.Entry<Suggest.Suggestion.Entry.Option> suggestion, String... text) {
        for (String s : text) {
            addOption(suggestion, s);
        }
    }


    private void addOption(Suggest.Suggestion.Entry<Suggest.Suggestion.Entry.Option> suggestion, String text) {
        suggestion.addOption(new Suggest.Suggestion.Entry.Option(new Text(text), 0.5f));

    }

}
