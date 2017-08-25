package nl.vpro.domain.api.thesaurus;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.extern.slf4j.Slf4j;
import nl.vpro.beeldengeluid.gtaa.GTAARepository;
import nl.vpro.domain.media.gtaa.GTAAPerson;
import nl.vpro.jackson2.Jackson2Mapper;

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
     * @param jws
     *            is unpacked, key checked, converted to a gtaa-person and submitted
     *            to the {@link GTAAService}
     */
    @Override
    public GTAAPerson submitPerson(String jws) {
        return saveGTAAPerson(jws);
    }

    /**
     * Used for testing
     *
     * @param p
     *            the Person to encrypt
     * @return a JWT string
     */

    protected String encryptPerson(GTAAPerson p, String issuer, String key, String user) {
        ObjectMapper mapper = Jackson2Mapper.getLenientInstance();
        Map<String, Object> claims = mapper.convertValue(p, mapType);
        String compactJws = Jwts.builder().setSubject("GTAAPerson").setHeaderParam("iss", issuer)
                .setHeaderParam("usr", user).setClaims(claims).signWith(SignatureAlgorithm.HS512, key.getBytes())
                .compact();
        log.debug(compactJws);
        return compactJws;
    }

    private GTAAPerson saveGTAAPerson(String jws) {
        JwtParser parser = Jwts.parser().setSigningKeyResolver(keyResolver);
        Jws<Claims> claims = parser.parseClaimsJws(StringUtils.trim(jws));
        String creator = getCreator(claims.getHeader());
        return gtaaService.submit(convertClaimsToPerson(claims), creator);
    }

    private String getCreator(JwsHeader<?> header) {
        Assert.notNull(header.get("usr"), "Expecting a 'usr' value in the header " + header);
        return header.get("iss") + ":" + header.get("usr");
    }

    private GTAAPerson convertClaimsToPerson(Jws<Claims> claims) {
        ObjectMapper mapper = Jackson2Mapper.getLenientInstance();
        Claims body = claims.getBody();
        body.remove("iat");
        return mapper.convertValue(body, GTAAPerson.class);
    }

}
