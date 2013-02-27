package nl.vpro.api.service.disqus;

import nl.vpro.api.transfer.DisqusThreadInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ernst Bunders
 */
@Component
public class DummyDisqusInfoListener implements DisqusInfoListener {

    private static final Logger LOG = LoggerFactory.getLogger(DummyDisqusInfoListener.class);

    @Override
    public void threadInfoUpdate(String siteName, String identifier, DisqusThreadInfo info) {
        LOG.debug(String.format("Notification of disqus thread info update. thread %s in site %s: %s", identifier, siteName, info.toString()));

    }
}
