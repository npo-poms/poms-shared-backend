package nl.vpro.domain.api.thesaurus;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

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

    @Autowired
    private GTAARepository gtaaService;

    @Autowired
    private GTAAKeysRepository keysRepo;

    @SuppressWarnings("rawtypes")
    private SigningKeyResolver keyResolver = new SigningKeyResolverAdapter() {
        @Override
        public byte[] resolveSigningKeyBytes(JwsHeader header, Claims claims) {
            Assert.notNull(header.get("iss"), "Expecting an issuer under key 'iss' in the header " + header);
            return keysRepo.getKeyFor((String) header.get("iss"))
                    .orElseThrow(() -> new RuntimeException("Couldn't find key for issuer " + header.get("iss")))
                    .getBytes();
        }
    };

    private final MapType mapType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class,
            Object.class);

    /**
     * @param jws is unpacked, key checked, converted to a gtaa-person and submitted
     *            to the {@link GTAAService}
     */
    @Override
    public GTAAPerson submitPerson(NewPerson newPerson, String jws) {
        JwtParser parser = Jwts.parser().setSigningKeyResolver(keyResolver);
        Jws<Claims> claims = parser.parseClaimsJws(StringUtils.trim(jws));
        String creator = getCreator(claims.getHeader());
        return gtaaService.submit(convertToPerson(newPerson), creator);
    }

    /**
     * Used for testing
     * @return a JWT string
     */

    protected String encrypt(String issuer, String key, String user) {
        String compactJws = Jwts.builder().setSubject("GTAAPerson").setHeaderParam("iss", issuer)
                .setHeaderParam("usr", user).signWith(SignatureAlgorithm.HS512, key.getBytes())
                .compact();
        log.debug(compactJws);
        return compactJws;
    }


    private String getCreator(JwsHeader<?> header) {
        Assert.notNull(header.get("usr"), "Expecting a 'usr' value in the header " + header);
        return header.get("iss") + ":" + header.get("usr");
    }

    private GTAAPerson convertToPerson(NewPerson newPerson) {
        GTAAPerson person = new GTAAPerson(Person.builder().givenName(newPerson.getGivenName()).familyName(newPerson.getFamilyName()).build());
        if(StringUtils.isNotBlank(newPerson.getNote())) {
            person.setNotes(Arrays.asList(Label.forValue(newPerson.getNote())));
        }
        return person;

    }
}
