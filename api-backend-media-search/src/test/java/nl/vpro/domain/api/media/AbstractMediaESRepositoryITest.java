package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.api.AbstractESRepositoryITest;
import nl.vpro.domain.api.SearchResultItem;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.elasticsearch.Constants;
import nl.vpro.elasticsearchclient.ElasticSearchIterator;
import nl.vpro.elasticsearchclient.IndexHelper;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.logging.Slf4jHelper;
import nl.vpro.media.domain.es.*;
import nl.vpro.util.Truthiness;

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
        IndexHelper indexName = createIndexIfNecessary(APIMEDIA);
        createIndexIfNecessary(APIMEDIA_REFS, indexName + ApiRefsIndex.POSTFIX);
        for (ApiCueIndex i : ApiCueIndex.getInstances()) {
            createIndexIfNecessary(i);
        }
    }

    /**
     * Testing wheter implemetation via es corresponds with predicate implementation of form itself.
     */
    protected void testResult(MediaForm form, MediaSearchResult result) {
        Set<String> inResult = new HashSet<>();

        //  check every result, they must be in the form
        for (SearchResultItem<? extends MediaObject> sr : result) {
            MediaSearch.TestResult testResult = form.getTestResult(sr.getResult());
            log.info("Asserting that {} is in form", sr.getResult());
            assertThat(testResult.test().getAsBoolean()).withFailMessage("But it is not! " +  testResult.getDescription()).isTrue();
            inResult.add(sr.getResult().getMid());
        }
        ElasticSearchIterator<MediaObject> i = new ElasticSearchIterator<>(staticClientFactory.get(),
            (jn) -> {
                try {
                    return Jackson2Mapper.getLenientInstance().treeToValue(jn.get(Constants.Fields.SOURCE), MediaObject.class);
                } catch (JsonProcessingException e) {
                    log.error(e.getMessage(), e);
                    return null;
                }
            });
        i.prepareSearch(indexHelpers.get(ApiMediaIndex.APIMEDIA).getIndexName());
        AtomicLong misses = new AtomicLong(0);
        i.forEachRemaining(mo -> {
            if (Workflow.PUBLICATIONS.contains(mo.getWorkflow())) {
                if (!inResult.contains(mo.getMid())) {
                    MediaSearch.TestResult testResult = form.getTestResult(mo);
                    Slf4jHelper.debugOrInfo(log, misses.get()  == 0, "Asserting that {} is not in form", mo);
                    assertThat(testResult.test())
                        .withFailMessage(form + " matched " + mo + " but it is not found! :" +
                            testResult.getDescription()
                        ).isNotEqualTo(Truthiness.TRUE);
                    misses.incrementAndGet();
                }
            }
        });
        log.info("hits {}, misses {}\nfor form\n{}", inResult.size(), misses.get(), form);
    }


    protected MediaSearchResult getAndTestResult(ESMediaRepository target, MediaForm form) {
        MediaSearchResult result = target.find(null, form, 0, null);
        testResult(form, result);
        return result;
    }

}
