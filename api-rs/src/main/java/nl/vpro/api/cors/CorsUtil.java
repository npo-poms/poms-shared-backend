/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.cors;

import org.apache.commons.lang.StringUtils;

/**
 * User: rico
 * Date: 02/05/2012
 */
public class CorsUtil {

    private enum simpleMethods {
        GET,
        HEAD,
        POST
    }
    public static boolean isSimpleMethod(String method) {
        if (StringUtils.isNotEmpty(method)) {
            if (simpleMethods.valueOf(method)!=null) {
                return true;
            }
        }
        return false;
    }
}
