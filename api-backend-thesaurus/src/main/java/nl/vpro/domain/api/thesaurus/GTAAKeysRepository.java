package nl.vpro.domain.api.thesaurus;

import java.io.IOException;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.NonNull;


/**
 * Every 'issuer' needs to have a key. These should be stored somewhere, and an implementation of this interface arranges that.
 */
public interface GTAAKeysRepository {

    Optional<String> getKeyFor(@NonNull String issuer) throws IOException;

}
