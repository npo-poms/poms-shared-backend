package nl.vpro.domain.api.thesaurus;

import java.io.IOException;
import java.util.Optional;

public interface GTAAKeysRepository {

    Optional<String> getKeyFor(String issuer) throws IOException;

}
