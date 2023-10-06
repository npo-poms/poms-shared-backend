package nl.vpro.domain.api.thesaurus;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Date;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import javax.crypto.SecretKey;

import org.assertj.core.api.Assertions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.meeuw.math.time.TestClock;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.vpro.domain.gtaa.*;

import static org.assertj.core.api.Assertions.assertThat;
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
@ExtendWith(MockitoExtension.class)
@Slf4j
public class GTAAServiceImplTest {


    private static final Clock clock = TestClock.twentyTwenty();


    private static final String SECRET_KEY = "ohzohj8Jwu1gieShciecev6Keiy3peiSteehuYa0sooFei4iCieV5rooeeB3eeZu";

    @Mock
    GTAARepository gtaa;

    @Mock
    GTAAKeysRepository keysRepo;

    GTAAServiceImpl gtaaService;
    @BeforeEach
    public void init() {
        gtaaService = new GTAAServiceImpl(gtaa, keysRepo);
        gtaaService.setClock(clock);
    }

    @Test
    public void testSubmitPersonWithValidToken() throws IOException {
        when(keysRepo.getKeyFor("demo-app")).thenReturn(Optional.of(SECRET_KEY));
        GTAANewPerson newPerson = GTAANewPerson
            .builder()
            .givenName("piet")
            .familyName("hein")
            .scopeNote("opmerking")
            .build();
        String jws = encrypt("demo-app", SECRET_KEY, "m.meeuwissen@vpro.nl", 10);
        assertThat(jws).isEqualTo("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwidXNyIjoibS5tZWV1d2lzc2VuQHZwcm8ubmwiLCJpYXQiOjE1ODIxOTA0MDAsImlzcyI6ImRlbW8tYXBwIiwiZXhwIjoxNTgyMjMzNjAwfQ.kv_GnoHWu90m23yt6XwAKYuDrsf4-kkZjWpmtuOwLl0");
        gtaaService.submitGTAAPerson(newPerson, jws);

        verify(gtaa).submit(any(GTAANewPerson.class), eq("demo-app"));
    }

    @Test
    public void testSubmitPersonWithExpiredToken() throws IOException {
        when(keysRepo.getKeyFor("demo-app")).thenReturn(Optional.of(SECRET_KEY));

        Assertions.assertThatThrownBy(() -> {

            GTAANewPerson newPerson = GTAANewPerson.builder()
                .givenName("piet")
                .familyName("hein")
                .scopeNote("opmerking")
                .build();
            String jws = encrypt("demo-app", SECRET_KEY, "m.meeuwissen@vpro.nl", 13);
            gtaaService.submitGTAAPerson(newPerson, jws);
        }).isInstanceOf(SecurityException.class);
    }
    @Test
    public void testAddPersonWithoutIssuer() {
        GTAANewPerson newPerson = GTAANewPerson.builder().givenName("piet").familyName("hein").scopeNote("opmerking").build();
        Assertions.assertThatThrownBy(() -> {
            String jws = encrypt(null, SECRET_KEY, "user y", 13);
            gtaaService.submitGTAAPerson(newPerson, jws);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Below methods just used for testing
     *
     * @return a JWT string
     */

    protected static String _encrypt(@NonNull  String issuer, @NonNull String key, @NonNull String user, int issuedBeforeHours) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", issuer);
        claims.put("usr", user);
        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes());
        String compactJws = Jwts.builder()
            .subject("test")
            .claims(claims)
            .signWith(secretKey)
            .issuedAt(Date.from(clock.instant().minus(issuedBeforeHours, ChronoUnit.HOURS)))
            .compact();
        log.debug(compactJws);
        return compactJws;
    }

    protected static String encrypt(
        @Nullable String issuer,
        @NonNull String key,
        @NonNull String user,
        int issuedBeforeHours) {
        SecretKey secretKey = Keys.hmacShaKeyFor(key.getBytes());
        return Jwts.builder()
            .subject("test")
            .claim("usr", user)
            .issuedAt(Date.from(clock.instant().minus(issuedBeforeHours, ChronoUnit.HOURS)))
            .issuer(issuer)
            .expiration(java.util.Date.from(clock.instant().plus(Duration.ofHours(2))))
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
    }

}
