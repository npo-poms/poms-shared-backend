/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;

import nl.vpro.api.Settings;
import nl.vpro.domain.api.AbstractESRepositoryITest;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.media.domain.es.ApiMediaIndex;
import nl.vpro.media.domain.es.MediaESType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author r.jansen
 */
@Slf4j
public class ESMediaRepositoryFlushDelayITest extends AbstractMediaESRepositoryITest {

    private static final ESMediaRepository target = new ESMediaRepository((s) -> client, "tags");

    private long indexerStartTime;
    private long indexerStopTime;
    private long foundDataTime;
    private long foundAllDataTime;

    // Number of documents to index
    private int DOCCOUNT = 1000;

    @Override
    protected void firstRun() throws Exception {
        createIndexIfNecessary(ApiMediaIndex.NAME);

        target.mediaRepository = mock(MediaRepository.class);
        target.settings = new Settings();
        when(target.mediaRepository.redirect(anyString())).thenReturn(Optional.empty());

        target.setIndexName(indexName);
        clearIndex();
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

    private Callable<Long> indexer() throws Exception {
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

    private Callable<Long> querier() throws Exception {
        return () -> {
            long count = 0;
            log.info("Querier start");
            while (count < DOCCOUNT) {
                final SearchRequestBuilder searchRequestBuilder = AbstractMediaESRepositoryITest.client.prepareSearch(indexName)
                    .addSort("publishDate", SortOrder.ASC)
                    .addSort("mid", SortOrder.ASC)
                    .setTypes(MediaESType.mediaObjects());
                ActionFuture<SearchResponse> searchResponseFuture = AbstractMediaESRepositoryITest.client
                    .search(searchRequestBuilder.request());

                SearchResponse response = searchResponseFuture.actionGet(5000, TimeUnit.MILLISECONDS);
                SearchHits hits = response.getHits();
                count = hits.getTotalHits();
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

    private static <T extends MediaObject> T index(T object) throws IOException, ExecutionException, InterruptedException {
        AbstractESRepositoryITest.client
            .index(
                new IndexRequest(indexName, getTypeName(object), object.getMid())
                    .source(Jackson2Mapper.INSTANCE.writeValueAsBytes(object), XContentType.JSON)
            ).get();
        assertThat(object.getLastPublishedInstant()).isNotNull();
        return object;
    }
}
