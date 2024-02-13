package nl.vpro.domain.api.thesaurus;

import io.jsonwebtoken.*;
import io.jsonwebtoken.lang.Assert;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.Clock;
import java.time.*;
import java.util.Date;

import jakarta.inject.Inject;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

import nl.vpro.domain.gtaa.*;
import nl.vpro.util.DateUtils;

/**
 * Wraps the {@link GTAARepository} to accept signed JWT to ensure the sender is
 * known. This allows anyone who has a known signing key to add GTAA people and other concepts.
 *
 * @author machiel
 */
@Service
@Log4j2
public class GTAAServiceImpl implements GTAAService {

    private final GTAARepository gtaaRepository;

    private final GTAAKeysRepository keysRepo;

    private final Duration maxAge = Duration.ofHours(12);

    @Setter
    @Getter
    private Clock clock = java.time.Clock.systemUTC();

   private final SigningKeyResolver keyResolver = new SigningKeyResolverAdapter() {

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
    public GTAAServiceImpl(GTAARepository gtaaRepository, GTAAKeysRepository keysRepo) {
        this.gtaaRepository = gtaaRepository;
        this.keysRepo = keysRepo;
    }

    /**
     * @param jws is unpacked, key checked, converted to a gtaa-person and will now be submitted
     *            to the {@link GTAARepository}. Extra check on expiration date (jwt should not be issued > 12h ago)
     */
    @Override
    public GTAAPerson submitGTAAPerson(@NonNull GTAANewPerson newPerson, @NonNull String jws) {
        String issuer = authenticate(jws);
        return gtaaRepository.submit(newPerson, issuer);
    }

    @Override
    public  <T extends GTAAConcept, S extends GTAANewConcept>  T submitGTAAConcept(@NonNull S newObject, @NonNull String jws) {
        String issuer = authenticate(jws);
        return gtaaRepository.submit(newObject, issuer);
    }


    private String authenticate(@NonNull String jws) throws SecurityException{
         JwtParser parser = Jwts.parser()
            .clockSkewSeconds(5)
            .setSigningKeyResolver(keyResolver)
            .clock(() -> Date.from(clock.instant()))
            .build();

        Jws<Claims> claims = parser.parseSignedClaims(jws);
        String issuer = claims.getPayload().getIssuer();

        Instant issuedAt = DateUtils.toInstant(claims.getPayload().getIssuedAt());
        Instant maxAllowed = clock.instant().minus(maxAge);
        if (issuedAt == null) {
            throw new SecurityException("JWT token didn't have an issued at time");
        }
        if (issuedAt.isBefore(maxAllowed)) {
            throw new SecurityException("JWT token was issued more than then permitted " + maxAge + " ago");
        }
        log.debug("JWS authenticated {} for subject {}", issuer, claims.getPayload().getSubject());
        return issuer;
    }




}
