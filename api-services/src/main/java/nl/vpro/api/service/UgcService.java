package nl.vpro.api.service;

import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.domain.ugc.playerconfiguration.PlayerConfiguration;

/**
 * Date: 24-4-12
 * Time: 13:11
 *
 * @author Ernst Bunders
 */
public interface UgcService {
    Annotation getAnnotation(String id);

    Annotation getAnnotiationByPart(String id);

    /**
     * @param id The couchDB-id (32 char UUID) or URN of the playerConfiguration
     * @return a found playerConfiguration
     * @throws nl.vpro.util.rs.error.NotFoundException if not found
     * @throws nl.vpro.util.rs.error.ServerErrorException if any other error occurred
     */
    PlayerConfiguration getPlayerConfiguration(String id);

    /**
     * Insert a playerConfig. Field "id" must be empty.
     * @param playerConfiguration
     * @return
     */
    PlayerConfiguration insertPlayerConfiguration(PlayerConfiguration playerConfiguration);

}
