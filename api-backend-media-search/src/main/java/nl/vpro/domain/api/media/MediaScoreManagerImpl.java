/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.*;
import org.springframework.stereotype.Component;

import nl.vpro.domain.api.AbstractConfigFileScoreManager;

/**
 * @author Roelof Jan Koekoek
 * @since 4.2
 */
@ManagedResource(objectName = "nl.vpro.api:name=MediaScoreManager")
@Component
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
    public Long getSortDateScale() {
        return ESMediaScoreBuilder.sortDateScale;
    }

    @ManagedOperation
    @Override
    public void setSortDateScale(Long sortDateScale) {
        ESMediaScoreBuilder.sortDateScale = sortDateScale;
    }

    @ManagedAttribute
    @Override
    public Long getSortDateOffset() {
        return ESMediaScoreBuilder.sortDateOffset;
    }

    @ManagedOperation
    @Override
    public void setSortDateOffset(Long sortDateOffset) {
        ESMediaScoreBuilder.sortDateOffset = sortDateOffset;
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
        final Properties properties = loadConfig();
        for(Map.Entry<Object, Object> entry : properties.entrySet()) {
            if(entry.getKey().toString().startsWith(TEXT_BOOST_PREFIX)) {
                ESMediaQueryBuilder.boostField(entry.getKey().toString().substring(TEXT_BOOST_PREFIX.length()), getBoost(entry));
            } else {
                switch(entry.getKey().toString()) {
                    case "boost.max":
                        setMaxBoost(getBoost(entry));
                    case "boost.type.series":
                        setSeriesBoost(getBoost(entry));
                    case "boost.type.broadcast":
                        setBroadcastBoost(getBoost(entry));
                    case "boost.location":
                        setLocationBoost(getBoost(entry));
                    case "sortDate.decay":
                        setSortDateDecay(getBoost(entry));
                    case "sortDate.scale":
                        setSortDateScale(getLong(entry));
                    case "sortDate.offset":
                        setSortDateOffset(getLong(entry));
                }
            }

        }
    }
}
