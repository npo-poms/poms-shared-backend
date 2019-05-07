package nl.vpro.domain.api.thesaurus;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import nl.vpro.domain.media.gtaa.GTAANewPerson;
import nl.vpro.domain.media.gtaa.GTAAPerson;
import nl.vpro.domain.media.gtaa.GTAARepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Wraps the GTAAService allowing {@link GTAAPerson}s to be submitted using JWT
 *
 * @author machiel
 * @since 5.4
 */
@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class JWTGTAAServiceImplTest {

    private static String SECRET_KEY = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

    @Mock
    GTAARepository gtaa;

    @Mock
    GTAAKeysRepository keysRepo;

    JWTGTAAServiceImpl jwtService;
    @Before
    public void init() {
        jwtService = new JWTGTAAServiceImpl(gtaa, keysRepo);
    }

    @Test
    public void testSubmitPersonWithValidToken() throws IOException {
        when(keysRepo.getKeyFor("demo-app")).thenReturn(Optional.of(SECRET_KEY));
        GTAANewPerson newPerson = GTAANewPerson
            .builder()
            .givenName("piet")
            .familyName("hein")
            .note("opmerking")
            .build();
        String jws = encrypt1("demo-app", SECRET_KEY, "user y");
        jwtService.submit(newPerson, jws);

        verify(gtaa).submit(any(GTAANewPerson.class), eq("demo-app"));
    }

    @Test (expected = SecurityException.class)
    public void testSubmitPersonWithExpiredToken() throws IOException {
        when(keysRepo.getKeyFor("demo-app")).thenReturn(Optional.of(SECRET_KEY));
        GTAANewPerson newPerson = GTAANewPerson.builder()
        .givenName("piet")
            .familyName("hein")
            .note("opmerking")
            .build();
        String jws = encrypt2("demo-app", SECRET_KEY, "user y");
        jwtService.submit(newPerson, jws);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddPersonWithoutIssuer() {
        GTAANewPerson newPerson = GTAANewPerson.builder().givenName("piet").familyName("hein").note("opmerking").build();
        String jws = encrypt2(null, SECRET_KEY, "user y");
        jwtService.submit(newPerson, jws);
    }

    /**
     * Below methods just used for testing
     *
     * @return a JWT string
     */

    protected static String encrypt1(String issuer, String key, String user) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("iss", issuer);
        claims.put("usr", user);
        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes());
        String compactJws = Jwts.builder()
            .setSubject("GTAAPerson")
            .setClaims(claims)
            .signWith(secretKey)
            .setIssuedAt(Date.from(Instant.now().minus(10, ChronoUnit.HOURS)))
            .compact();
        log.debug(compactJws);
        return compactJws;
    }

    protected static String encrypt2(String issuer, String key, String user) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("iss", issuer);
        claims.put("usr", user);

        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes());

        String compactJws = Jwts.builder()
            .setSubject("GTAAPerson")
            .setClaims(claims)
            .signWith(secretKey)
            .setIssuedAt(Date.from(Instant.now().minus(13, ChronoUnit.HOURS)))
            .compact();
        log.debug(compactJws);
        return compactJws;
    }


}
