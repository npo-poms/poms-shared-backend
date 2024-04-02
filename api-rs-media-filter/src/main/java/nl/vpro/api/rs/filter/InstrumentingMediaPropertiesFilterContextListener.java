/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.filter;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * Sets up {@link MediaPropertiesFilters} which is used for properties filtering.
 * <p>
 * This needs to be the first class loading the relevant classes
 *
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class InstrumentingMediaPropertiesFilterContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        MediaPropertiesFilters.instrument();
    }
}
