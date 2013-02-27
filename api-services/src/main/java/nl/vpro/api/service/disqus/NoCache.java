package nl.vpro.api.service.disqus;

import com.google.common.base.Optional;

import java.io.Serializable;

/**
 * Neutral implementation for transparently disabling caching.
 *
 * @author Ernst Bunders
 */
public class NoCache implements CacheWrapper {
    @Override
    public Optional<Serializable> get(Serializable key) {
        return Optional.absent();
    }

    @Override
    public void put(Serializable key, Serializable value) {
    }

    @Override
    public Optional<Long> getCreationTime(Serializable key) {
        return Optional.absent();
    }
}
