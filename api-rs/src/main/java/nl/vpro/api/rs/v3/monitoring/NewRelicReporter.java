/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.monitoring;

/**
 * @author Roelof Jan Koekoek
 * @since 3.7
 */
public interface NewRelicReporter {

    String getName();

    void pollCycle();

}
