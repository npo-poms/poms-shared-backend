package nl.vpro.domain.api.thesaurus;

import java.io.IOException;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface GTAAKeysRepository {

    Optional<String> getKeyFor(@NonNull String issuer) throws IOException;

}
