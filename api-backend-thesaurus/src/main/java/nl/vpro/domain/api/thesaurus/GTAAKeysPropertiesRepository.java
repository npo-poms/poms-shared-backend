package nl.vpro.domain.api.thesaurus;

import java.util.Optional;
import java.util.Properties;

public class GTAAKeysPropertiesRepository implements GTAAKeysRepository {

    private final Properties keys;

    public GTAAKeysPropertiesRepository(Properties keys) {
        this.keys = keys;
    }

    @Override
    public Optional<String> getKeyFor(String issuer) {
        return Optional.ofNullable(keys.getProperty(issuer));
    }

}
