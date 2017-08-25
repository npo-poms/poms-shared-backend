package nl.vpro.domain.api.thesaurus;

import java.util.Optional;

public interface GTAAKeysRepository {

    Optional<String> getKeyFor(String issuer);

}
