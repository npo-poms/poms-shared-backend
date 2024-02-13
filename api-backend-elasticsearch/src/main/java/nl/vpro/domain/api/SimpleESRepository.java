/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.persistence.PrePersist;
import jakarta.validation.*;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.springframework.stereotype.Repository;

import nl.vpro.domain.Identifiable;
import nl.vpro.elasticsearch.ElasticSearchIndex;
import nl.vpro.elasticsearch.highlevel.HighLevelClientFactory;

/**
 * A repository that talks to exactly one type in exactly one index
 * <p>
 * Support also {@link PrePersist}, but needing {@code jakarta.persistence-api} dependency then.
 * @author Michiel Meeuwisssen
 */
@SuppressWarnings("resource")
@Log4j2
@Repository
public class SimpleESRepository<T extends Identifiable<I>, I extends Serializable & Comparable<I>> extends AbstractESRepository<T> {

    @Getter
    private final ElasticSearchIndex index;

    private final Class<T> clazz;

    private Method prePersist;

    @Inject
    public SimpleESRepository(HighLevelClientFactory factory, Class<T> clazz, ElasticSearchIndex apiElasticSearchIndex) {
        super(factory);
        this.setIndexName(apiElasticSearchIndex, apiElasticSearchIndex.getIndexName());
        this.index = apiElasticSearchIndex;
        this.clazz = clazz;
        for (Method method :clazz.getMethods()) {
            if (method.getAnnotation(PrePersist.class) != null) {
                prePersist = method;
            }
        }
    }

    @Override
    protected ElasticSearchIndex getIndex(String id, Class<?> clazz) {
        return index;
    }

    public String getIndexName() {
        return indexNames.get(index);
    }

    public void setIndexName(@NonNull String indexName) {
        setIndexName(index, indexName);
    }

    public String getPublishIndexName() {
        return getIndexName();
    }

    public T save(T update) {
        if (prePersist != null) {
            try {
                prePersist.invoke(update);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<T>> result = factory.getValidator().validate(update);
            if (result.size() > 0) {
                throw  new ConstraintViolationException(result.toString(), result);
            }
        } catch (NoProviderFoundException npfe) {
            log.warn("Could not validate because {}", npfe.getMessage());
        }
        try {
            IndexRequest request = new IndexRequest(getPublishIndexName());
            request.id(update.getId().toString());
            request.source(mapper.writeValueAsBytes(update), XContentType.JSON);
            IndexResponse response = client().index(request, RequestOptions.DEFAULT);
            log.info("Indexed {} {}", update, response.getVersion());
            return update;
        } catch (ElasticsearchException  e) {
            log.warn("Error during update {} {}", update, e.getDetailedMessage());
            return null;
        } catch (IOException e) {
            log.error("Error saving {}} {} {}", clazz.getSimpleName(), update, e.getMessage());
            return null;
        }

    }

    public Optional<T> get(I id) throws IOException {
        GetRequest request = new GetRequest(getIndexName(), id.toString());
        GetResponse response = client().get(request, RequestOptions.DEFAULT);
        if (response.isExists()) {
            return Optional.of(unMap(response.getSourceAsString()));
        }
        return Optional.empty();
    }


    public boolean delete(I id) throws IOException {
        DeleteRequest request = new DeleteRequest(getIndexName(), id.toString());
        DeleteResponse response =
            client().delete(request, RequestOptions.DEFAULT);
        log.info("Deleted {} {} ({})", id, response.getVersion(), response.status());
        return response.status() != RestStatus.NOT_FOUND;
    }

    protected T unMap(String string) {
        try {
            return mapper.readValue(string, clazz);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(string + ":" + e.getMessage(), e);
        }
    }

}
