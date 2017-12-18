package nl.vpro.domain.api.thesaurus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

import javax.annotation.PostConstruct;

import org.springframework.core.io.Resource;


@Slf4j
@NoArgsConstructor
public class GTAAKeysPropertiesRepository implements GTAAKeysRepository {


    @Getter
    @Setter
    private List<Resource> locations;


    @PostConstruct
    public void init() throws IOException {
        Set<Object> keys = new HashSet<>();
        for (Resource location : locations) {
            keys.addAll(getProperties(location).keySet());
        }
        log.info("Reading gtaa issuer keys from {} (currently known keys {})", locations, keys);
    }



    @Override
    public Optional<String> getKeyFor(String issuer) {

         for (Resource location : locations ) {
            try {
                log.debug("Searching key for {} in {}...", issuer, location);
                String key = getProperties(location).getProperty(issuer);
                if (key != null) {
                    log.debug("Key found for {} in {}", issuer, location);
                    return Optional.of(key);
                }
            } catch (FileNotFoundException fne) {
                continue;
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

        }
        return Optional.empty();
    }

    private  Properties getProperties(Resource inputStream) throws IOException {
        Properties props = new Properties();
        try {
            props.load(inputStream.getInputStream());

        } catch (IOException e) {
            log.error("Failed to load properties file");
            return props;
        }

        return props;
    }


}
