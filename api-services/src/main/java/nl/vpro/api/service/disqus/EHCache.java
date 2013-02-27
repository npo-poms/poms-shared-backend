package nl.vpro.api.service.disqus;

import com.google.common.base.Optional;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.Serializable;

/**
 * @author Ernst Bunders
 */
@Component
public class EHCache implements CacheWrapper {
    public static final String CACHE_NAME = "disqus-thread-details";

    private final long diskExpiryThreadIntervalSeconds = Cache.DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS;

    private final MemoryStoreEvictionPolicy evictionPolicy = MemoryStoreEvictionPolicy.LRU;
    private final CacheManager cacheManager;

    @Autowired
    public EHCache(@Value("${disqus.cache.cachePath}") String cachePath,
                   @Value("${disqus.cache.maxElementsInMemory}") int maxElementsInMemory) {

        //why do this? EHCache will read ehcache-config.xml and replace the property with the value, if it's a system property
        System.setProperty("ehcache.disk.store.dir", cachePath);
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("ehcache-config.xml");

        cacheManager = CacheManager.create(is);
        Cache memoryOnlyCache = new Cache(CACHE_NAME, maxElementsInMemory, true, true, 0, 0, true, diskExpiryThreadIntervalSeconds);
        cacheManager.addCache(memoryOnlyCache);
    }

    @Override
    public Optional<Serializable> get(Serializable key) {
        final Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache.isKeyInCache(key)) {
            return Optional.of((Serializable) cache.get(key).getObjectValue());
        }
        return Optional.absent();
    }

    @Override
    public void put(Serializable key, Serializable value) {
        cacheManager.getCache(CACHE_NAME).put(new Element(key, value));
    }

    @Override
    public Optional<Long> getCreationTime(Serializable key) {
        final Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache.isKeyInCache(key)) {
            Element element = cache.get(key);
            return Optional.of(element.getCreationTime());
        }
        return Optional.absent();
    }
}
