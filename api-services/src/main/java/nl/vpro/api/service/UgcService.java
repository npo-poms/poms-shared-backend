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
    public Annotation getAnnotation(String id);

    public Annotation getAnnotiationByPart(String id);

    public PlayerConfiguration getPlayerConfiguration(String id);

    public PlayerConfiguration insertPlayerConfiguration(PlayerConfiguration playerConfiguration);

    public PlayerConfiguration updatePlayerConfiguration(PlayerConfiguration playerConfiguration);

}
