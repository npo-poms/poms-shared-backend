package nl.vpro.api.service;

import nl.vpro.api.transfer.DisqusThreadInfo;

/**
 * Get the disqus thread info for a collection of pages. Some things can go wrong:
 * - The api limit can be reached. In that case we are going to stop querying the api
 * for a predefined amount of time.
 * - There is no thread for a given id.
 * When this happens {@link DisqusThreadInfo#infoUnavailable} is returned for that id.
 *
 * @author Ernst Bunders
 */
public interface DisqusService {
    public DisqusThreadInfo getThreadInfo(String siteName, String identifier);
}
