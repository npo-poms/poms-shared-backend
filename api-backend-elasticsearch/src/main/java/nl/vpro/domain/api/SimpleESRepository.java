/*
 * Copyright (C) 2016 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.PrePersist;
import javax.validation.*;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;

import nl.vpro.domain.Identifiable;
import nl.vpro.elasticsearch.ESClientFactory;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.poms.es.ApiElasticSearchIndex;

/**
 * A repository that talks to exactly one type in exaclty one index
 * @author Michiel Meeuwisssen
 */
@Slf4j
@Repository
public class SimpleESRepository<T extends Identifiable<I>, I extends Serializable> extends AbstractESRepository<T> {

    @Getter
    private final ApiElasticSearchIndex index;

    private final Class<T> clazz;

    private Method prePersist;

    @Inject
    public SimpleESRepository(ESClientFactory factory, Class<T> clazz, ApiElasticSearchIndex apiElasticSearchIndex) {
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
    protected ApiElasticSearchIndex getIndex(String id, Class clazz) {
        return index;
    }

    public String getIndexName() {
        return indexNames.get(index);
    }

    public void setIndexName(@NonNull String indexName) {
        setIndexName(index, indexName);
    }


    public T save(T update) {
        if (prePersist != null) {
            try {
                prePersist.invoke(update);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Set<ConstraintViolation<T>> result = factory.getValidator().validate(update);
            if (result.size() > 0) {
                throw  new ConstraintViolationException(result.toString(), result);
            }
        } catch (NoProviderFoundException npfe) {
            log.warn("Could not validate because {}", npfe.getMessage());
        }
        try {
            IndexResponse response = client()
                .prepareIndex().setIndex(getIndexName()).setId(update.getId().toString())
                .setSource(Jackson2Mapper.getInstance().writeValueAsBytes(update), XContentType.JSON).get();
            if (response.status() == RestStatus.ACCEPTED) {
                log.info("Indexed {} {}", update, response.getVersion());
                return update;
            } else {
                log.warn("Error during update {} {}", update, response.status());
                return null;
            }
        } catch (JsonProcessingException e) {
            log.error("Error saving {}} {} {}", clazz.getSimpleName(), update, e.getMessage());
            return null;
        }

    }

    public Optional<T> get(I id) {
        GetResponse response = client().prepareGet().setIndex(getIndexName()).setId(id.toString()).get();
        if (response.isExists()) {
            return Optional.of(unMap(response.getSourceAsString()));
        }
        return Optional.empty();
    }


    public boolean delete(I id) {
        DeleteResponse response =
            client()
                .prepareDelete().setIndex(getIndexName()).setId(id.toString())
                .get();
        log.info("Deleted {} {} ({})", id, response.getVersion(), response.status());
        return response.status() != RestStatus.NOT_FOUND;

    }

    protected T unMap(String string) {
        try {
            return Jackson2Mapper.getInstance().readValue(string, clazz);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(string + ":" + e.getMessage(), e);
        }
    }



}
