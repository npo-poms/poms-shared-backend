package nl.vpro.api.service;

import com.google.common.base.Optional;
import nl.vpro.api.service.disqus.CacheWrapper;
import nl.vpro.api.service.disqus.DisqusInfoListener;
import nl.vpro.api.service.disqus.SimpleCache;
import nl.vpro.api.transfer.DisqusThreadInfo;
import org.junit.Test;
import org.springframework.web.client.RestOperations;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Ernst Bunders
 */
public class DisqusServiceImplTest {
    final static String site = "s";
    final static String id = "12334";
    final static String apikey = "x";
    final static int postCount = 3;
    private final Optional<DisqusInfoListener> noListener = Optional.absent();
    private final Optional<DisqusServiceImpl> noService = Optional.absent();


    @Test
    public void testGetThreadInfo_noFetchOnCacheHit() throws Exception {
        CacheWrapper cache = cacheHit();

        //throw an exception when data is fetched.
        RestOperations restOps = mock(RestOperations.class);
        when(restOps.getForObject(anyString(), eq(Map.class), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("This should not be called"));

        runTest(cache, noListener, restOps, noService);
    }

    @Test
    public void testGetThreadInfo_fetchOnCacheMiss() throws Exception {
        CacheWrapper cache = cacheMiss();

        //create the rest template
        RestOperations restOps = mock(RestOperations.class);
        when(restOps.getForObject(anyString(), eq(Map.class), anyString(), anyString(), anyString()))
                .thenReturn(getAvailableResult());

        runTest(cache, noListener, restOps, noService);
        verify(restOps).getForObject(anyString(), eq(Map.class), anyString(), anyString(), anyString());
    }

    @Test
    public void testGetThreadInfo_notifyOnCacheMiss() throws Exception {
        CacheWrapper cache = cacheMiss();

        DisqusInfoListener listener = mock(DisqusInfoListener.class);
        RestOperations restOps = mock(RestOperations.class);
        when(restOps.getForObject(anyString(), eq(Map.class), anyString(), anyString(), anyString()))
                .thenReturn(getAvailableResult());

        runTest(cache, Optional.of(listener), restOps, noService);

        verify(listener).threadInfoUpdate(anyString(), anyString(), any(DisqusThreadInfo.class));

    }

    @Test
    public void testGetThreadInfo_noNotifyOnCacheHit() throws Exception {
        CacheWrapper cache = cacheHit();

        DisqusInfoListener listener = mock(DisqusInfoListener.class);
        RestOperations restOps = mock(RestOperations.class);
        when(restOps.getForObject(anyString(), eq(Map.class), anyString(), anyString(), anyString()))
                .thenReturn(getAvailableResult());

        //throw exception on notification
        doThrow(new RuntimeException("Result fetched from the cache should not be cached"))
                .when(listener).threadInfoUpdate(anyString(), anyString(), any(DisqusThreadInfo.class));

        runTest(cache, Optional.of(listener), restOps, noService);
    }

    @Test
    public void testGetThreadInfo_availableResultShouldBeCached() {
        testCaching(getAvailableResult());
    }

    @Test
    public void testGetThreadInfo_unavailableResultShouldBeCached() {
        testCaching(getUnavailableResult());
    }

    @Test
    public void testGetThreadInfo_limitExceededResultShouldBeCached() {
        testCaching(getLimitExceededResult());
    }

    private void testCaching(Map<String, Object> result) {
        CacheWrapper cache = cacheMiss();

        RestOperations restOps = mock(RestOperations.class);
        when(restOps.getForObject(anyString(), eq(Map.class), anyString(), anyString(), anyString()))
                .thenReturn(result);

        runTest(cache, noListener, restOps, noService);
        assertNotNull(cache.get(site + id));
    }


    @Test
    public void testGetThreadInfo_resultFromCacheShouldNotBeCached() {
        CacheWrapper cache = mock(CacheWrapper.class);

        when(cache.get(site + id))
                .thenReturn(Optional.<Serializable>of(new DisqusThreadInfo(postCount)));
        when(cache.getCreationTime(any(Serializable.class)))
                .thenReturn(Optional.of(System.currentTimeMillis()));

        doThrow(new RuntimeException("This should not be called"))
                .when(cache).put(any(Serializable.class), any(Serializable.class));

        RestOperations restOps = mock(RestOperations.class);

        //run the test
        runTest(cache, noListener, restOps, noService);
    }


    @Test
    public void testGetThreadInfo_unavailableShouldNotOverwriteAvailableInCache() {
        testCacheOverwrite(getUnavailableResult());
    }

    @Test
    public void testGetThreadInfo_limitExceedsShouldNotOverwriteAvailableInCache() {
        testCacheOverwrite(getLimitExceededResult());
    }

    private void testCacheOverwrite(Map<String, Object> result) {
        CacheWrapper cache = cacheHitExpired();

        RestOperations restOps = mock(RestOperations.class);
        when(restOps.getForObject(anyString(), eq(Map.class), anyString(), anyString(), anyString()))
                .thenReturn(result);

        runTest(cache, noListener, restOps, noService);

        DisqusThreadInfo cachedItem = (DisqusThreadInfo) cache.get(site + id).get();
        assertFalse("available cached expired item overwritten with unavailable item",
                DisqusThreadInfo.INFO_UNAVAILABLE.equals(cachedItem));
    }

    @Test
    public void testGetThreadInfo_pauseOnLimitExceeded() {
        //first create a limit exceeded request (to start the pause
        CacheWrapper cache = cacheMiss();

        //the request will return an 'limit exceed' result.
        RestOperations restOps = mock(RestOperations.class);
        when(restOps.getForObject(anyString(), eq(Map.class), anyString(), anyString(), anyString()))
                .thenReturn(getLimitExceededResult());

        DisqusServiceImpl service = runTest(cache, noListener, restOps, noService);
        DisqusThreadInfo cachedInfo = (DisqusThreadInfo) cache.get(site + id).get();
        assertTrue("We should have the 'unavailable' in the cache", DisqusThreadInfo.INFO_UNAVAILABLE.equals(cachedInfo));

        //then run a second request, make sure no RestOperation is called.
        cache = cacheMiss();

        restOps = mock(RestOperations.class);
        when(restOps.getForObject(anyString(), eq(Map.class), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("No requests during pause."));

        runTest(cache, noListener, restOps, Optional.of(service));
    }


    private DisqusServiceImpl runTest(CacheWrapper cache, Optional<DisqusInfoListener> listenerOption, RestOperations restOps, Optional<DisqusServiceImpl> serviceOption) {
        DisqusServiceImpl dsi;
        if (serviceOption.isPresent()) {
            dsi = serviceOption.get();
        } else {
            dsi = serviceOption.isPresent() ? serviceOption.get() : new DisqusServiceImpl();
            dsi.setRateLimitExceededPauseInSeconds(900);
            dsi.setTimeToLiveSeconds(14400);
            dsi.setUnavailableTimeToLiveSeconds(900);
            dsi.setApiKey(apikey);
        }

        dsi.setRestTemplate(restOps);
        dsi.setCacheWrapper(cache);
        if (listenerOption.isPresent()) {
            dsi.setDisqusInfoListener(listenerOption.get());
        }

        dsi.getThreadInfo(site, id);
        return dsi;
    }

    private Map<String, Object> getAvailableResult() {
        return baseResult(0, Optional.of(postCount));
    }

    private Map<String, Object> getUnavailableResult() {
        return baseResult(8, Optional.<Integer>absent());
    }

    private Map<String, Object> getLimitExceededResult() {
        return baseResult(13, Optional.<Integer>absent());
    }


    private Map<String, Object> baseResult(int code, Optional<Integer> posts) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", code);
        if (posts.isPresent()) {
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("posts", posts.get());
            result.put("response", response);
        }
        return result;
    }

    private CacheWrapper cacheHit() {
        CacheWrapper cache = new SimpleCache();
        cache.put(site + id, new DisqusThreadInfo(postCount));
        return cache;
    }

    private CacheWrapper cacheHitExpired() {
        SimpleCache cache = new SimpleCache();
        cache.put(site + id, new DisqusThreadInfo(postCount), 0);
        return cache;
    }

    private CacheWrapper cacheMiss() {
        return new SimpleCache();
    }

}
