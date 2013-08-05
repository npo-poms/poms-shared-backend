/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.swagger;

import javax.servlet.ServletConfig;

import com.wordnik.swagger.jaxrs.ConfigReader;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class SwaggerConfigReader extends ConfigReader {

    private static String basePath = "http://rs-test.vpro.nl/v2/api";

    public SwaggerConfigReader() {
    }

    public SwaggerConfigReader(ServletConfig config) {
        // For Resteasy init call
    }

    public void setBasePath(String basePath) {
        SwaggerConfigReader.basePath = basePath;
    }

    @Override
    public String basePath() {
        return basePath;
    }

    @Override
    public String swaggerVersion() {
        return "1.2";
    }

    @Override
    public String apiVersion() {
        return "2.0";
    }

    @Override
    public String modelPackages() {
        return null;
    }

    @Override
    public String apiFilterClassName() {
        return null;
    }
}
