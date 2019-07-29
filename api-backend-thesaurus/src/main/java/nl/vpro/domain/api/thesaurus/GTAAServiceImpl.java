package nl.vpro.domain.api.thesaurus;

import io.jsonwebtoken.*;
import io.jsonwebtoken.lang.Assert;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

import nl.vpro.domain.gtaa.*;
import nl.vpro.util.DateUtils;

/**
 * Wraps the {@link GTAARepository} to accept signed JWT to ensure the sender is
 * known. This allows anyone who has a known signing key to add GTAA people.
 *
 * @author machiel
 */
@Service
@Slf4j
public class GTAAServiceImpl implements GTAAService {

    private final GTAARepository gtaaService;

    private final GTAAKeysRepository keysRepo;

    private Duration maxAge = Duration.ofHours(12);


    private SigningKeyResolver keyResolver = new SigningKeyResolverAdapter() {

        @Override
        public byte[] resolveSigningKeyBytes(@NonNull JwsHeader header, @NonNull Claims claims) {
            Assert.notNull(claims.get("iss"), "no value for 'iss' available");
            try {
                return keysRepo.getKeyFor((String) claims.get("iss"))
                    .orElseThrow(() -> new SecurityException("Couldn't find key for issuer " + claims.get("iss")))
                    .getBytes();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            return null;
        }
    };

    @Inject
    public GTAAServiceImpl(GTAARepository gtaaService, GTAAKeysRepository keysRepo) {
        this.gtaaService = gtaaService;
        this.keysRepo = keysRepo;
    }

    /**
     * @param jws is unpacked, key checked, converted to a gtaa-person and will now be submitted
     *            to the {@link GTAARepository}. Extra check on expiration date (jwt should not be issued > 12h ago)
     */
    @Override
    public GTAAPerson submit(@NonNull GTAANewPerson newPerson, @NonNull String jws) {
        String issuer = authenticate(jws);
        return gtaaService.submit(newPerson, issuer);
    }

    @Override
    public  <T extends GTAAConcept, S extends GTAANewConcept>  T submit(@NonNull S newObject, @NonNull String jws) {
        String issuer = authenticate(jws);
        return gtaaService.submit(newObject, issuer);
    }


    private String authenticate(@NonNull String jws) throws SecurityException{
        JwtParser parser = Jwts.parser().setSigningKeyResolver(keyResolver);
        parser.setAllowedClockSkewSeconds(5);

        Jws<Claims> claims = parser.parseClaimsJws(jws);
        String issuer = claims.getBody().getIssuer();

        Instant issuedAt = DateUtils.toInstant(claims.getBody().getIssuedAt());
        Instant maxAllowed = Instant.now().minus(maxAge);
        if (issuedAt == null) {
            throw new SecurityException("JWT token didn't have an issued at time");
        }
        if (issuedAt.isBefore(maxAllowed)) {
            throw new SecurityException("JWT token was issued more than the permitted " + maxAge + " ago");
        }
        log.debug("JWS authenticated {} for subject {}", issuer, claims.getBody().getSubject());
        return issuer;
    }




}
