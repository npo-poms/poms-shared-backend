/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
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
        return ESMediaScoreBuilder.sortDateScale.toString();
    }

    @ManagedOperation
    @Override
    public void setSortDateScale(String  sortDateScale) {
        ESMediaScoreBuilder.sortDateScale = TimeUtils.parseDuration(sortDateScale).orElseThrow(IllegalArgumentException::new);
    }

    @ManagedAttribute
    @Override
    public String getSortDateOffset() {
        return ESMediaScoreBuilder.sortDateOffset.toString();
    }

    @ManagedOperation
    @Override
    public void setSortDateOffset(String sortDateOffset) {
        ESMediaScoreBuilder.sortDateOffset = TimeUtils.parseDuration(sortDateOffset).orElseThrow(IllegalArgumentException::new);
    }

    @ManagedAttribute
    @Override
    public double getSortDateDecay() {
        return ESMediaScoreBuilder.sortDateDecay;
    }

    @ManagedOperation
    @Override
    public void setSortDateDecay(double sortDateDecay) {
        ESMediaScoreBuilder.sortDateDecay = sortDateDecay;
    }

    @ManagedAttribute
    @Override
    public float getLocationBoost() {
        return ESMediaScoreBuilder.locationBoost;
    }

    @ManagedOperation
    @Override
    public void setLocationBoost(float locationBoost) {
        ESMediaScoreBuilder.locationBoost = locationBoost;
    }

    @ManagedAttribute
    @Override
    public float getSeriesBoost() {
        return ESMediaScoreBuilder.seriesBoost;
    }

    @ManagedOperation
    @Override
    public void setSeriesBoost(float seriesBoost) {
        ESMediaScoreBuilder.seriesBoost = seriesBoost;
    }

    @ManagedAttribute
    @Override
    public float getBroadcastBoost() {
        return ESMediaScoreBuilder.broadcastBoost;
    }

    @ManagedOperation
    @Override
    public void setBroadcastBoost(float broadcastBoost) {
        ESMediaScoreBuilder.broadcastBoost = broadcastBoost;
    }

    @ManagedAttribute
    @Override
    public float getMaxBoost() {
        return ESMediaScoreBuilder.maxBoost;
    }

    @ManagedOperation
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
                        case "boost.max":
                            setMaxBoost(Float.parseFloat(entry.getValue()));
                            break;
                        case "boost.type.series":
                            setSeriesBoost(Float.parseFloat(entry.getValue()));
                            break;
                        case "boost.type.broadcast":
                            setBroadcastBoost(Float.parseFloat(entry.getValue()));
                            break;
                        case "boost.location":
                            setLocationBoost(Float.parseFloat(entry.getValue()));
                            break;
                        case "sortDate.decay":
                            setSortDateDecay(Float.parseFloat(entry.getValue()));
                            break;
                        case "sortDate.scale":
                            setSortDateScale(entry.getValue());
                            break;
                        case "sortDate.offset":
                            setSortDateOffset(entry.getValue());
                            break;
                        default:
                            log.warn("Unrecognized entry {}", entry);
                            continue;
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

}
