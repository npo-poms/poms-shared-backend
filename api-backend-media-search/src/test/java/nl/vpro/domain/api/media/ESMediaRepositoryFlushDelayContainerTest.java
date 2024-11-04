/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;

import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.elasticsearchclient.IndexHelper;
import nl.vpro.media.domain.es.ApiMediaIndex;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author r.jansen
 */
@Log4j2
public class ESMediaRepositoryFlushDelayContainerTest extends AbstractMediaESRepositoryContainerTest {

    private static ESMediaRepository target;

    private long indexerStartTime;
    private long indexerStopTime;
    private long foundDataTime;
    private long foundAllDataTime;

    // Number of documents to index
    private static final int DOCCOUNT = 1000;

    @Override
    protected void firstRun() {
        target = new ESMediaRepository(staticClientFactory, "tags", new MediaScoreManagerImpl());
        createIndicesIfNecessary();
        target.setIndexName(indexHelpers.get(ApiMediaIndex.APIMEDIA).getIndexName());
        clearIndices();
    }

    @Test
    public void testIndexingDelay() throws Exception {

        List<Callable<Long>> callables = Arrays.asList(
            indexer(),
            querier()
        );

        ExecutorService executor = Executors.newFixedThreadPool(callables.size());

        List<Long> results = executor.invokeAll(callables)
            .stream()
            .map(future -> {
                try {
                    return future.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).collect(Collectors.toList());

        assertThat(results.size()).isEqualTo(2);
        final long flushDelay = foundAllDataTime - indexerStopTime;
        final long firstResultDelay = foundDataTime - indexerStartTime;
        log.info("Delay to first result : {}ms", firstResultDelay);
        log.info("Delay to all results : {}ms", flushDelay);
        log.info("Total indexing time : {}ms", (indexerStopTime - indexerStartTime));
        assertThat(Duration.ofMillis(flushDelay)).isLessThan(target.getCommitDelay());
        assertThat(Duration.ofMillis(firstResultDelay)).isLessThan(target.getCommitDelay());
    }

    private Callable<Long> indexer() {
        return () -> {
            log.info("Indexer start");
            indexerStartTime = System.currentTimeMillis();
            for (int i = 0; i < DOCCOUNT; i++) {
                MediaTestDataBuilder.ProgramTestDataBuilder programBuilder = MediaTestDataBuilder
                    .program()
                    .lastPublished(Instant.now())
                    .workflow(Workflow.PUBLISHED)
                    .lastModified(Instant.now())
                    .mid("ID_" + i)
                    .constrained()
                    .withImages();
                index(programBuilder.build());
            }
            indexerStopTime = System.currentTimeMillis();
            log.info("Indexer done");
            return indexerStopTime;
        };
    }

    private Callable<Long> querier() {
        return () -> {
            long count = 0;
            log.info("Querier start");
            while (count < DOCCOUNT) {
                IndexHelper helper = indexHelpers.get(ApiMediaIndex.APIMEDIA);
                SearchSourceBuilder source = new SearchSourceBuilder();



                source.sort("publishDate", SortOrder.ASC)
                    .sort("mid", SortOrder.ASC);

                final SearchRequest searchRequest = new SearchRequest(helper.getIndexName());
                searchRequest.source(source);

                SearchResponse response = highLevelClient().search(searchRequest, RequestOptions.DEFAULT);
                SearchHits hits = response.getHits();
                count = hits.getTotalHits().value;
                if (count > 0 && foundDataTime == 0) {
                    log.info("Found first document");
                    foundDataTime = System.currentTimeMillis();
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {
                }
            }
            foundAllDataTime = System.currentTimeMillis();
            log.info("Querier done");
            return foundAllDataTime;
        };
    }

    private  <T extends MediaObject> T index(T object) throws IOException, ExecutionException, InterruptedException {
        indexHelpers.get(ApiMediaIndex.APIMEDIA).index(object.getMid(), object);
        return object;
    }
}
