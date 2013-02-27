package nl.vpro.api.service;

import com.google.common.base.Optional;
import nl.vpro.api.service.disqus.*;
import nl.vpro.api.transfer.DisqusThreadInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class fetches data from the Disqus api service.
 * Because we have a limit of 1000 requests per second it relies heavily on caching.
 * For a request there are two possible outcomes: 'available' and 'unavailable. Both outcomes will translate to
 * a {@link DisqusThreadInfo} object. unavailable is reference to {@link DisqusThreadInfo#infoUnavailable}
 * <p/>
 * The caching mechanism treats the outcomes different:
 * - if the outcome is 'available' the cached item will be cached for the number of seconds set with {@link #timeToLiveSeconds}
 * - if the outcome is 'unavailable' the cached item will be cached for the number of seconds set with {@link #unavailableTimeToLiveSeconds}
 * <p/>
 * If the request limit has been reached, the service will lay of sending requests for a time set in seconds
 * with {@link #rateLimitExceededPauzeInSeconds}. During that time all requests will result in {@link DisqusThreadInfo#infoUnavailable}
 * <p/>
 * The caching mechanism will not replace 'unavailable' results with 'available' results. in this situation the cached
 * version is reinserted into the cache, to reset the creation time.
 *
 * @author Ernst Bunders
 */
@Service("disqusService")
public final class DisqusServiceImpl implements DisqusService {

    private static final Logger LOG = LoggerFactory.getLogger(DisqusServiceImpl.class);

    static final String URL_THREAD_DETAILS = "https://disqus.com/api/3.0/threads/details.json?api_key={key}&thread:ident={id}&forum={site}";

    @Autowired
    private RestOperations restTemplate;

    @Autowired
    private CacheWrapper cacheWrapper = new NoCache();

    @Value("${disqus.rateLimit.exceeded.pauseInSeconds}")
    private long rateLimitExceededPauzeInSeconds;

    @Value("${disqus.cache.timeToLiveSeconds}")
    private long timeToLiveSeconds;

    @Value("${disqus.cache.unavailable.timeToLiveSeconds}")
    private long unavailableTimeToLiveSeconds;

    @Autowired
    private DisqusInfoListener disqusInfoListener = new DummyDisqusInfoListener();

    @Value("${disqus.apikey}")
    private String apiKey;

    private AtomicLong waitUntil = new AtomicLong(-1);


    @Override
    public DisqusThreadInfo getThreadInfo(final String siteName, final String identifier) {
        final String key = createKey(siteName, identifier);
        if (shouldFetch(key)) {
            DisqusThreadInfo info = fetchResultUntilPause(siteName, identifier);

            if (shouldUpdateCache(key, info)) {
                disqusInfoListener.threadInfoUpdate(siteName, identifier, info);
                cacheWrapper.put(key, info);
            }
        }

        return (DisqusThreadInfo) cacheWrapper.get(key).get();
    }

    private DisqusThreadInfo fetchResultUntilPause(final String siteName, final String identifier) {
        if (noPauseSet() || pauseExpired()) {
            try {
                return fetchResult(siteName, identifier);
            } catch (RateLimitExceededException e) {
                LOG.warn("Rate limit exceeded!");
                setPause();
                return DisqusThreadInfo.infoUnavailable;
            }
        }
        return DisqusThreadInfo.infoUnavailable;
    }

    private boolean shouldUpdateCache(String key, DisqusThreadInfo info) {
        if (!cacheWrapper.get(key).isPresent())
            return true;

        final DisqusThreadInfo cachedInfo = (DisqusThreadInfo) cacheWrapper.get(key).get();

        //if the currently cached info is 'available' and the new info is not, we keep the old version, give it a new lease.
        if (isAvailable(cachedInfo) && !isAvailable(info)) {
            //reinsert it to reset the creation time
            cacheWrapper.put(key, cachedInfo);
            return false;
        }
        return true;
    }

    /**
     * An item should be fetched if it is not in the cache, or it has expired.
     * We use different time to live values for 'available' and 'unavailable' items.
     *
     * @param key the cache key
     */
    private boolean shouldFetch(String key) {
        final Optional<Serializable> cachedInfoOption = cacheWrapper.get(key);
        if (!cachedInfoOption.isPresent()) {
            LOG.debug("Cache miss on {}", key);
            return true;
        }

        final DisqusThreadInfo cachedInfo = (DisqusThreadInfo) cachedInfoOption.get();

        //'Available' info has a different time to live in the cache (probably longer)
        final long secondsToCacheThis = isAvailable(cachedInfo) ? timeToLiveSeconds : unavailableTimeToLiveSeconds;
        final long cacheCreationTime = cacheWrapper.getCreationTime(key).get();

        //Expired?
        if (cacheCreationTime + (1000 * secondsToCacheThis) < System.currentTimeMillis()) {
            LOG.debug("Cache item {} has expired.", key);
            return true;
        }

        return false;
    }

    private boolean isAvailable(DisqusThreadInfo cachedInfo) {
        return !DisqusThreadInfo.infoUnavailable.equals(cachedInfo);
    }

    private boolean pauseExpired() {
        if (waitUntil.get() < System.currentTimeMillis()) {
            waitUntil.set(-1); /*pause is over*/
            return true;
        }
        return false;
    }

    private boolean noPauseSet() {
        return waitUntil.get() == -1;
    }

    private void setPause() {
        LOG.debug("Will not fetch data for {} seconds", rateLimitExceededPauzeInSeconds);
        waitUntil.set(System.currentTimeMillis() + (1000 * rateLimitExceededPauzeInSeconds));
    }

    /**
     * This method tries to fetch the info from disqus.
     * It will always return a DisqusThreadInfo instance, it can be {@link DisqusThreadInfo#infoUnavailable}
     *
     * @throws RateLimitExceededException when the rate limit is exceeded. We should lay of the requests for a while
     */
    @SuppressWarnings("unchecked")
    private DisqusThreadInfo fetchResult(final String siteName, final String identifier) throws RateLimitExceededException {
        try {
            Map<String, Object> response = restTemplate.getForObject(URL_THREAD_DETAILS, Map.class, apiKey, identifier, siteName);
            int statusCode = getStatusCode(response);
            if (succes(statusCode)) {
                return extractResult(response);
            } else {
                if (rateLimitExceeded(statusCode)) {
                    throw new RateLimitExceededException();
                } else {
                    //any other error code. {@link http://disqus.com/api/docs/errors/}
                    LOG.warn(String.format("Fetching disqus data for thread %s in site %s failed. Error code %s", identifier, siteName, "" + statusCode));
                }
            }
        } catch (RestClientException e) {
            LOG.error(String.format("Something went wrong fetching info for thread %s in site %s: %s", identifier, siteName, e.getMessage()));
        }
        return DisqusThreadInfo.infoUnavailable;
    }

    private boolean rateLimitExceeded(int statusCode) {
        return 13 == statusCode || 14 == statusCode;
    }

    private boolean succes(int statusCode) {
        return 0 == statusCode;
    }

    private int getStatusCode(Map<String, Object> response) {
        return (Integer) response.get("code");
    }

    @SuppressWarnings("unchecked")
    private DisqusThreadInfo extractResult(final Map<String, Object> response) {
        Map<String, Object> data = (Map<String, Object>) response.get("response");
        int posts = data.get("posts") != null ? (Integer) data.get("posts") : 0;
        return new DisqusThreadInfo(posts);
    }


    private String createKey(final String siteName, final String identifier) {
        return siteName + identifier;
    }


    public void setRestTemplate(RestOperations restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setCacheWrapper(CacheWrapper cacheWrapper) {
        this.cacheWrapper = cacheWrapper;
    }

    public void setRateLimitExceededPauseInSeconds(long rateLimitExceededPauzeInSeconds) {
        this.rateLimitExceededPauzeInSeconds = rateLimitExceededPauzeInSeconds;
    }

    public void setTimeToLiveSeconds(long timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    public void setUnavailableTimeToLiveSeconds(long unavailableTimeToLiveSeconds) {
        this.unavailableTimeToLiveSeconds = unavailableTimeToLiveSeconds;
    }

    public void setDisqusInfoListener(DisqusInfoListener disqusInfoListener) {
        this.disqusInfoListener = disqusInfoListener;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
