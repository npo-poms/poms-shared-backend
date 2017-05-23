/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import nl.vpro.domain.api.ScoreManager;

/**
 * @author Roelof Jan Koekoek
 * @since 4.2
 */
public interface MediaScoreManager extends ScoreManager {

    Long getSortDateScale();

    void setSortDateScale(Long sortDateScale);

    Long getSortDateOffset();

    void setSortDateOffset(Long sortDateOffset);

    double getSortDateDecay();

    void setSortDateDecay(double sortDateDecay);

    float getLocationBoost();

    void setLocationBoost(float locationBoost);

    float getSeriesBoost();

    void setSeriesBoost(float seriesBoost);

    float getBroadcastBoost();

    void setBroadcastBoost(float broadcastBoost);

    float getMaxBoost();

    void setMaxBoost(float maxBoost);
}
