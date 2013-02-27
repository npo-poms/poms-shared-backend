package nl.vpro.api.service.disqus;

import com.google.common.base.Optional;

import java.io.Serializable;

/**
 * @author Ernst Bunders
 */
public interface CacheWrapper {

    public Optional<Serializable> get(Serializable key);

    public void put(Serializable key, Serializable value);

    public Optional<Long> getCreationTime(Serializable key);
}
