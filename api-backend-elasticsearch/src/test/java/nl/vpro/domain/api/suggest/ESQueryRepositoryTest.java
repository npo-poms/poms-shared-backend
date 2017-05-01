package nl.vpro.domain.api.suggest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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

        String input = "luba";
        SuggestResult adapt = instance.adapt(suggest, input, null);
        assertThat(adapt.getItems()).hasSize(5);
        int prevDistance = Integer.MIN_VALUE;
        for (Suggestion sug : adapt.getItems()) {
            int distance = StringUtils.getLevenshteinDistance(input, sug.getText());
            System.out.println(distance + ":" + sug.getText());
            assertThat(distance).isGreaterThanOrEqualTo(prevDistance);
            prevDistance = distance;
        }


    }

}
