package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.AbstractESRepositoryITest;
import nl.vpro.domain.api.SearchResultItem;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.elasticsearch7.ElasticSearchIterator;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.media.domain.es.*;

import static nl.vpro.media.domain.es.ApiMediaIndex.APIMEDIA;
import static nl.vpro.media.domain.es.ApiRefsIndex.APIMEDIA_REFS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 5.5
 */
@Slf4j
public abstract class AbstractMediaESRepositoryITest extends AbstractESRepositoryITest {

    protected static void createIndicesIfNecessary() {
        String indexName = createIndexIfNecessary(APIMEDIA);
        createIndexIfNecessary(APIMEDIA_REFS, indexName + ApiRefsIndex.POSTFIX);
        for (ApiCueIndex i : ApiCueIndex.getInstances()) {
            createIndexIfNecessary(i);
        }
    }


    /**
     * Testing wheter implemetation via es corresponds with predicate implemtation of form itself.
     */

    protected void testResult(MediaForm form, MediaSearchResult result) {
        Set<String> inResult = new HashSet<>();
        for (SearchResultItem<? extends MediaObject> sr : result) {
            MediaSearch.TestResult testResult = form.getTestResult(sr.getResult());
            log.info("Asserting that {} is in {}", sr.getResult(), form);
            assertThat(testResult.test()).withFailMessage("But it is not! " +  testResult.getDescription()).isTrue();
            inResult.add(sr.getResult().getMid());
        }
        ElasticSearchIterator<MediaObject> i = new ElasticSearchIterator<MediaObject>(client,
            (sh) -> {
                try {
                    return Jackson2Mapper.getLenientInstance().readValue(sh.getSourceAsString(), MediaObject.class);
                } catch (JsonProcessingException e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            });
        i.prepareSearch(indexNames.get(ApiMediaIndex.APIMEDIA));

        i.forEachRemaining(mo -> {
            if (! inResult.contains(mo.getMid())) {
                MediaSearch.TestResult testResult = form.getTestResult(mo);
                log.info("Asserting that {} is not in {}", mo, form);
                assertThat(testResult.test()).withFailMessage("But it is! :" +  testResult.getDescription()).isFalse();

            }
        });


        // TODO test that the rest or repo indeed is false
    }


    protected MediaSearchResult getAndTestResult(ESMediaRepository target, MediaForm form) {
        MediaSearchResult result = target.find(null, form, 0, null);
        testResult(form, result);
        return result;
    }



}
