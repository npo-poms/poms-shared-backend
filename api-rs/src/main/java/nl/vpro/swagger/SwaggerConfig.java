/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.swagger;

import javax.servlet.ServletConfig;

import com.wordnik.swagger.jaxrs.config.DefaultJaxrsConfig;

/**
 * @author Roelof Jan Koekoek
 * @since 2.3
 */
public class SwaggerConfig extends DefaultJaxrsConfig {
    @Override
    public void init(ServletConfig servletConfig) {
        super.init(servletConfig);
    }
}
