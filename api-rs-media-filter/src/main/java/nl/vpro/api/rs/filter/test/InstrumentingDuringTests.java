package nl.vpro.api.rs.filter.test;

import lombok.extern.log4j.Log4j2;

import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

import nl.vpro.api.rs.filter.MediaPropertiesFilters;

@Log4j2
public class InstrumentingDuringTests implements LauncherDiscoveryListener  {
    static {
        MediaPropertiesFilters.instrument();
    }

    @Override
	public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
        //MediaPropertiesFilters.instrument();

        // static was even earlier?
        log.info("{}", request);
    }

}
