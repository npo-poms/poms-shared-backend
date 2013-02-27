package nl.vpro.api.service.disqus;

import com.google.common.base.Optional;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple memory only cache impl.
 *
 * @author Ernst Bunders
 */

public class SimpleCache implements CacheWrapper {
    private final Map<Serializable, Serializable> cache = new HashMap<Serializable, Serializable>();
    private final Map<Serializable, Long> times = new HashMap<Serializable, Long>();


    @Override
    public Optional<Serializable> get(Serializable key) {
        if (cache.containsKey(key)) {
            return Optional.of(cache.get(key));
        }
        return Optional.absent();
    }

    @Override
    public void put(Serializable key, Serializable value) {
        cache.put(key, value);
        times.put(key, System.currentTimeMillis());
    }

    public void put(Serializable key, Serializable value, long cacheTime) {
        cache.put(key, value);
        times.put(key, cacheTime);
    }

    @Override
    public Optional<Long> getCreationTime(Serializable key) {
        if (times.containsKey(key)) {
            return Optional.of(times.get(key));
        }
        return Optional.absent();
    }

}
