package nl.vpro.domain.api.media;

import nl.vpro.domain.api.topspin.Recommendations;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
public interface TopSpinRepository {
    Recommendations getForMid(String mid);
}
