package nl.vpro.domain.api.thesaurus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

import javax.annotation.PostConstruct;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.core.io.Resource;


/**
 * The implementation of {@link GTAAKeysRepositoryImpl} backed by a (number of) property file(s).
 */
@Slf4j
@NoArgsConstructor
public class GTAAKeysRepositoryImpl implements GTAAKeysRepository {


    @Getter
    @Setter
    private List<Resource> locations;


    @PostConstruct
    public void init() {
        Set<Object> keys = new HashSet<>();
        for (Resource location : locations) {
            keys.addAll(getProperties(location).keySet());
        }
        log.info("Reading gtaa issuer keys from {} (currently known keys {})", locations, keys);
    }



    @Override
    public Optional<String> getKeyFor(@NonNull String issuer) {

         for (Resource location : locations ) {
             log.debug("Searching key for {} in {}...", issuer, location);
             String key = getProperties(location).getProperty(issuer);
             if (key != null) {
                 log.debug("Key found for {} in {}", issuer, location);
                 return Optional.of(key);
             }

        }
        return Optional.empty();
    }

    private  Properties getProperties(@NonNull Resource inputStream) {
        final Properties props = new Properties();
        try {
            if (inputStream.exists()) {
                if (inputStream.isReadable()) {
                    props.load(inputStream.getInputStream());
                } else {
                    log.info("{} exists but cannot be read", inputStream);
                }
            } else {
                log.info("{} does not exist", inputStream);
            }
        } catch (IOException e) {
            log.error("Failed to load properties file {}: {} {}", inputStream, e.getClass().getName(), e.getMessage(), e);
        }

        return props;
    }


}
