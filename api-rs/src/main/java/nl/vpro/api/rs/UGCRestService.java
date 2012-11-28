/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs;

import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.domain.ugc.playerconfiguration.PlayerConfiguration;

/**
 * User: rico
 * Date: 28/11/2012
 */
public interface UGCRestService {

    public PlayerConfiguration getPlayerConfiguration(String id);

    public PlayerConfiguration insertPlayerConfiguration(PlayerConfiguration playerConfiguration);

    public Annotation getAnnotation(String id);

    public Annotation getAnnotationByPart(String id);

}
