/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.*;
import org.springframework.stereotype.Component;

import nl.vpro.domain.api.AbstractConfigFileScoreManager;
import nl.vpro.domain.api.SearchFieldDefinition;
import nl.vpro.util.TimeUtils;

/**
 * @author Roelof Jan Koekoek
 * @since 4.2
 */
@ManagedResource(objectName = "nl.vpro.api:name=MediaScoreManager")
@Component
@Log4j2
public class MediaScoreManagerImpl extends AbstractConfigFileScoreManager implements MediaScoreManager {

    private static final String MEDIA_SCORES_FILE = "media.scores";

    @Value("${api.score.config.dir}")
    private String scoreConfigDir;


    @PostConstruct
    private void init() throws Exception {
        loadScores();
        watchConfig();
    }

    @ManagedOperation
    @Override
    public String getTextFieldBoosts() {
        return ESMediaQueryBuilder.getSearchFields().toString();
    }

    @ManagedOperation
    @ManagedOperationParameters({
        @ManagedOperationParameter(name = "field", description = "name of this field "),
        @ManagedOperationParameter(name = "boost", description = "the value to boost")
    })
    @Override
    public void setTextField(String field, float boost) {
        ESMediaQueryBuilder.boostField(field, boost);
    }

    @ManagedAttribute
    @Override
    public String getSortDateScale() {
        return ESMediaScoreBuilder.sortDate.getScale().toString();
    }

    @ManagedOperation
    @Override
    public void setSortDateScale(String  sortDateScale) {
        ESMediaScoreBuilder.sortDate.setScale(TimeUtils.parseDuration(sortDateScale).orElseThrow(IllegalArgumentException::new));
    }

    @ManagedAttribute
    @Override
    public String getSortDateOffset() {
        return ESMediaScoreBuilder.sortDate.getOffset().toString();
    }

    @ManagedOperation
    @Override
    public void setSortDateOffset(String sortDateOffset) {
        ESMediaScoreBuilder.sortDate.setOffset(TimeUtils.parseDuration(sortDateOffset).orElseThrow(IllegalArgumentException::new));
    }

    @ManagedAttribute
    @Override
    public double getSortDateDecay() {
        return ESMediaScoreBuilder.sortDate.getDecay();
    }

    @ManagedAttribute
    @Override
    public void setSortDateDecay(double sortDateDecay) {
        ESMediaScoreBuilder.sortDate.setDecay(sortDateDecay);
    }

    @ManagedAttribute
    @Override
    public double getSortDateGaussFactor() {
        return ESMediaScoreBuilder.sortDate.getGaussFactor();
    }

    @ManagedAttribute
    @Override
    public void setSortDateGaussFactor(double sortDateFactorFactor) {
        ESMediaScoreBuilder.sortDate.setGaussFactor(sortDateFactorFactor);
    }

    @ManagedAttribute
    @Override
    public double getSortDateGaussOffset() {
        return ESMediaScoreBuilder.sortDate.getGaussOffset();
    }

    @ManagedAttribute
    @Override
    public void setSortDateGaussOffset(double sortDateFactorOffset) {
        ESMediaScoreBuilder.sortDate.setGaussOffset(sortDateFactorOffset);
    }

    @ManagedAttribute
    @Override
    public float getLocationBoost() {
        return ESMediaScoreBuilder.locationBoost;
    }

    @ManagedAttribute
    @Override
    public void setLocationBoost(float locationBoost) {
        ESMediaScoreBuilder.locationBoost = locationBoost;
    }

    @ManagedAttribute
    @Override
    public float getSeriesBoost() {
        return ESMediaScoreBuilder.seriesBoost;
    }

    @ManagedAttribute
    @Override
    public void setSeriesBoost(float seriesBoost) {
        ESMediaScoreBuilder.seriesBoost = seriesBoost;
    }

    @ManagedAttribute
    @Override
    public float getBroadcastBoost() {
        return ESMediaScoreBuilder.broadcastBoost;
    }

    @ManagedAttribute
    @Override
    public void setBroadcastBoost(float broadcastBoost) {
        ESMediaScoreBuilder.broadcastBoost = broadcastBoost;
    }

    @ManagedAttribute
    @Override
    public float getMaxBoost() {
        return ESMediaScoreBuilder.maxBoost;
    }

    @ManagedAttribute
    @Override
    public void setMaxBoost(float maxBoost) {
        ESMediaScoreBuilder.maxBoost = maxBoost;
    }


    @Override
    protected String getConfigDir() {
        return scoreConfigDir;
    }

    @Override
    protected String getConfigFileName() {
        return MEDIA_SCORES_FILE;
    }

    @Override
    protected void loadScores() {
        final Map<String, String> properties = loadConfig();
        Set<String> unHandledFields  = ESMediaQueryBuilder.SEARCH_FIELDS.stream().map(SearchFieldDefinition::getName).collect(Collectors.toCollection(HashSet::new));
        Set<String> unHandledKeys  = new HashSet<>(properties.keySet());

        for(Map.Entry<String, String> entry : properties.entrySet()) {
            try {
                if (entry.getKey().startsWith(TEXT_BOOST_PREFIX)) {
                    String name = entry.getKey().substring(TEXT_BOOST_PREFIX.length());
                    if (ESMediaQueryBuilder.boostField(name, Float.parseFloat(entry.getValue()))) {
                        unHandledFields.remove(name);
                    } else {
                        log.warn("Unrecognized entry {}", entry);
                        continue;
                    }
                } else {
                    switch (entry.getKey()) {
                        case "boost.max" -> setMaxBoost(Float.parseFloat(entry.getValue()));
                        case "boost.type.series" -> setSeriesBoost(Float.parseFloat(entry.getValue()));
                        case "boost.type.broadcast" -> setBroadcastBoost(Float.parseFloat(entry.getValue()));
                        case "boost.location" -> setLocationBoost(Float.parseFloat(entry.getValue()));
                        case "sortDate.decay" -> setSortDateDecay(Float.parseFloat(entry.getValue()));
                        case "sortDate.scale" -> setSortDateScale(entry.getValue());
                        case "sortDate.offset" -> setSortDateOffset(entry.getValue());
                        case "sortDate.gaussFactor" -> setSortDateGaussFactor(Float.parseFloat(entry.getValue()));
                        case "sortDate.gaussOffset" -> setSortDateGaussOffset(Float.parseFloat(entry.getValue()));
                        default -> {
                            log.warn("Unrecognized entry {}", entry);
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("For {}: {}:{}", entry, e.getClass(), e.getMessage());
            }
            unHandledKeys.remove(entry.getKey());

        }
        if (! unHandledFields.isEmpty()) {
            log.info("Text fields not configured: {}", unHandledFields);
        }
        if (! unHandledKeys.isEmpty()) {
            log.info("Keys not configured: {}", unHandledKeys);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Method method : MediaScoreManagerImpl.class.getDeclaredMethods()) {
            if (method.getName().startsWith("get")) {
                try {
                    builder.append("\n").append(method.getName().substring(3)).append(": ").append(method.invoke(this));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return builder.toString();
    }

}
