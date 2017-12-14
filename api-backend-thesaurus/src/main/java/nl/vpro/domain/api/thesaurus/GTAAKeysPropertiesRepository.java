package nl.vpro.domain.api.thesaurus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.springframework.core.io.Resource;


@Slf4j
@NoArgsConstructor
public class GTAAKeysPropertiesRepository implements GTAAKeysRepository {


    @Getter
    @Setter
    private List<Resource> locations;


    @Override
    public Optional<String> getKeyFor(String issuer) throws IOException {

         for (Resource location : locations ) {
            try {
                File propfile = location.getFile();
                log.info("Searching key for " + issuer + " in" + propfile.getAbsolutePath() + "...");
                if (propfile.exists() && getKey(issuer, propfile) != null) {
                    log.info("Key found for " + issuer + " in" + propfile.getAbsolutePath());
                    return Optional.of(getKey(issuer, propfile));
                }
            } catch (IOException e) {
                e.printStackTrace();
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
