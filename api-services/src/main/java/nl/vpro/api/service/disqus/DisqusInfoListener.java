package nl.vpro.api.service.disqus;

import nl.vpro.api.transfer.DisqusThreadInfo;

/**
 * This interface represents an interest in changes in data that is fetched from disqus by the
 * {@link nl.vpro.api.service.DisqusServiceImpl}
 *
 * @author Ernst Bunders
 */
public interface DisqusInfoListener {
    public void threadInfoUpdate(String siteName, String identifier, DisqusThreadInfo info);
}
