package nl.vpro.domain.api.thesaurus;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Date;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import nl.vpro.domain.media.Person;
import nl.vpro.domain.media.gtaa.GTAAPerson;
import nl.vpro.domain.media.gtaa.GTAARepository;
import nl.vpro.openarchives.oai.Label;
import nl.vpro.rs.thesaurus.update.NewPerson;

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
                e.printStackTrace();
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
    public GTAAPerson submitPerson(NewPerson newPerson, String jws) {
        JwtParser parser = Jwts.parser().setSigningKeyResolver(keyResolver);
        parser.setAllowedClockSkewSeconds(5);
        Jws<Claims> claims = parser.parseClaimsJws(StringUtils.trim(jws));
        String creator = claims.getBody().getIssuer();
        Instant issuedAt = claims.getBody().getIssuedAt().toInstant();
        Instant maxAllowed = ZonedDateTime.now().minus(12, ChronoUnit.HOURS).toInstant();
        if (issuedAt.isBefore(maxAllowed)) {
            throw new SecurityException("JWT token was issued more than the permitted 12 hours ago");
        }
        return gtaaService.submit(convertToPerson(newPerson), creator);
    }

    private GTAAPerson convertToPerson(NewPerson newPerson) {
        GTAAPerson person = new GTAAPerson(Person.builder().givenName(newPerson.getGivenName()).familyName(newPerson.getFamilyName()).build());
        if(StringUtils.isNotBlank(newPerson.getNote())) {
            person.setNotes(Arrays.asList(Label.forValue(newPerson.getNote())));
        }
        return person;
    }


}
