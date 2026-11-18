package nl.vpro.domain.api.thesaurus;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.lang.Assert;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.security.Key;
import java.time.Clock;
import java.time.*;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        final byte[] keyBytes = keyFor(unverifiedIssuer(jws));
        JwtParser parser = Jwts.parser()
            .clockSkewSeconds(5)
            .keyLocator(header -> hmacKey(header, keyBytes))
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

    /**
     * A {@link Locator} only gets the header, but the key is determined by the 'iss' claim. So we peek in the (not yet verified) payload
     * to find it. The signature is still verified with the key found for it, so a forged issuer just results in a failing signature check.
     */
    private static String unverifiedIssuer(@NonNull String jws) {
        String[] parts = jws.split("\\.");
        if (parts.length != 3) {
            throw new MalformedJwtException("Not a JWS: expected 3 parts, found " + parts.length);
        }
        final JsonNode payload;
        try {
            payload = MAPPER.readTree(Decoders.BASE64URL.decode(parts[1]));
        } catch (IOException | RuntimeException e) {
            throw new MalformedJwtException("Couldn't parse JWS payload: " + e.getMessage(), e);
        }
        JsonNode iss = payload == null ? null : payload.get(Claims.ISSUER);
        Assert.isTrue(iss != null && iss.isTextual(), "no value for 'iss' available");
        return iss.asText();
    }

    private byte[] keyFor(@NonNull String issuer) {
        try {
            return keysRepo.getKeyFor(issuer)
                .orElseThrow(() -> new SecurityException("Couldn't find key for issuer " + issuer))
                .getBytes();
        } catch (IOException e) {
            throw new SecurityException("Couldn't load key for issuer " + issuer + ": " + e.getMessage(), e);
        }
    }

    private static Key hmacKey(@NonNull Header header, byte[] keyBytes) {
        String alg = header instanceof ProtectedHeader protectedHeader ? protectedHeader.getAlgorithm() : null;
        if (alg == null || !alg.startsWith("HS")) {
            throw new UnsupportedJwtException("Only HMAC signed tokens are supported, not " + alg);
        }
        // HS256 -> HmacSHA256, etc.
        return new SecretKeySpec(keyBytes, "HmacSHA" + alg.substring(2));
    }




}
