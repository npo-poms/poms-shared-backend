/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * User: rico
 * Date: 08/03/2012
 */

@Service("mediaService")
public class MediaServiceImpl implements MediaService {

    @Value("${mediaservice.api.url}")
    String apiUrl;

    public String get(String urn) {

        return urn;
    }

}
