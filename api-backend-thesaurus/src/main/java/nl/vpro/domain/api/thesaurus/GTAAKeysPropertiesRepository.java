package nl.vpro.domain.api.thesaurus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.core.io.Resource;


@Slf4j
@NoArgsConstructor
public class GTAAKeysPropertiesRepository implements GTAAKeysRepository {


    @Getter
    @Setter
    private List<Resource> locations;


    @PostConstruct
    public void init() {
        log.info("Reading gtaa issuer keys from {}", locations);
    }



    @Override
    public Optional<String> getKeyFor(String issuer) throws IOException {

         for (Resource location : locations ) {
            try {
                File propfile = location.getFile();
                log.debug("Searching key for {} in {}...", issuer, propfile.getAbsolutePath());
                if (! propfile.exists()) {
                    continue;
                }
                String key = getKey(issuer, propfile);
                if (key  != null) {
                    log.debug("Key found for {} in {}", issuer, propfile.getAbsolutePath());
                    return Optional.of(key);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

        }
        return Optional.empty();
    }


    public String getKey(String issuer, File file) throws IOException {
        FileReader reader = new FileReader(file);
        Properties props = new Properties();
        try {
            props.load(reader);

        } catch (IOException e) {
            log.info("Failed to load properties file");
            return null;
        }

        String key = props.getProperty(issuer);
        reader.close();
        return key;
    }

}
