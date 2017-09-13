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

    String getSortDateScale();

    void setSortDateScale(String sortDateScale);

    String getSortDateOffset();

    void setSortDateOffset(String sortDateOffset);

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
