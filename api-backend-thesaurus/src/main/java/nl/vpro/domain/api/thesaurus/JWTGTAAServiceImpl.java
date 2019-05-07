package nl.vpro.domain.api.thesaurus;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import nl.vpro.domain.media.gtaa.*;
import nl.vpro.util.DateUtils;

/**
 * Wraps the {@link GTAARepository} to accept signed JWT to ensure the sender is
 * known. This allows anyone who has a known signing key to add GTAA people.
 *
 * @author machiel
 */
@Service
@Slf4j
public class JWTGTAAServiceImpl implements JWTGTAAService {

    private final GTAARepository gtaaService;

    private final GTAAKeysRepository keysRepo;

    @SuppressWarnings("rawtypes")
    private SigningKeyResolver keyResolver = new SigningKeyResolverAdapter() {

        @Override
        public byte[] resolveSigningKeyBytes(JwsHeader header, Claims claims) {
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
    public JWTGTAAServiceImpl(GTAARepository gtaaService, GTAAKeysRepository keysRepo) {
        this.gtaaService = gtaaService;
        this.keysRepo = keysRepo;
    }

    /**
     * @param jws is unpacked, key checked, converted to a gtaa-person and will now be submitted
     *            to the {@link GTAARepository}. Extra check on expiration date (jwt should not be issued > 12h ago)
     */
    @Override
    public GTAAPerson submit(GTAANewPerson newPerson, String jws) {
        try {
            String issuer = authenticate(jws);
            return gtaaService.submit(newPerson, issuer);
        } catch (SecurityException se) {
            throw se;
        }
    }

    @Override
    public  <T extends ThesaurusObject, S extends NewThesaurusObject<T>>  T submit(S gtaaNewThesaurusObject, String jws) {
        try {
            String issuer = authenticate(jws);
            return gtaaService.submit(gtaaNewThesaurusObject, issuer);
        } catch (SecurityException se) {
            throw se;
        }
    }

    private String authenticate(String jws) throws SecurityException{
        JwtParser parser = Jwts.parser().setSigningKeyResolver(keyResolver);
        parser.setAllowedClockSkewSeconds(5);

        Jws<Claims> claims = parser.parseClaimsJws(jws);
        String issuer = claims.getBody().getIssuer();

        Instant issuedAt = DateUtils.toInstant(claims.getBody().getIssuedAt());
        Instant maxAllowed = Instant.now().minus(12, ChronoUnit.HOURS);
        if (issuedAt == null) {
            throw new SecurityException("JWT token didn't have an issued at time");
        }
        if (issuedAt.isBefore(maxAllowed)) {
            throw new SecurityException("JWT token was issued more than the permitted 12 hours ago");
        }
        log.debug("JWS authenticated {} for subject {}", issuer, claims.getBody().getSubject());
        return issuer;
    }




}
