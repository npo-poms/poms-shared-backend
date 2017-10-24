package nl.vpro.domain.api.thesaurus;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import nl.vpro.domain.media.gtaa.GTAAPerson;
import nl.vpro.domain.media.gtaa.GTAARepository;
import nl.vpro.rs.thesaurus.update.NewPerson;

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
    public void submitPersonShouldCallSubmitPersonOnGTAAMock() throws IOException {
        when(keysRepo.getKeyFor("backend-1")).thenReturn(Optional.of("SECRETABC"));

        NewPerson p = new NewPerson();
        String jws = encrypt("backend-1", "SECRETABC", "user x");
        jwtService.submitPerson(p, jws);

        verify(gtaa).submit(any(GTAAPerson.class), eq("backend-1:user x"));

    }

    @Test
    public void testGtaaUri() throws IOException {
        when(keysRepo.getKeyFor("backend-2")).thenReturn(Optional.of("SECRETCBA"));

        NewPerson p = new NewPerson();
        String jws = encrypt("backend-2", "SECRETCBA", "user y");
        jwtService.submitPerson(p, jws);

        verify(gtaa).submit(any(GTAAPerson.class), eq("backend-2:user y"));
    }

//    @Test
//    public void convertNewPersonToGTAAPerson() throws IOException {
//        JWTGTAAServiceImpl jsi = new JWTGTAAServiceImpl();
//        NewPerson p = new NewPerson("piet", "hein", "donner");
//        GTAAPerson g = jsi.submitPerson(p, "bla.bla.bla");
//        System.out.println(g);
//        assertThat(g.getGivenName()).isEqualTo("piet");
//    }

    @Test
    public void testNames() throws IOException {
        when(keysRepo.getKeyFor("backend")).thenReturn(Optional.of("SECRET"));

        NewPerson p = new NewPerson();
        p.setGivenName("Sjaak");
        p.setFamilyName("de Mul");

        String jws = encrypt("backend", "SECRET", "user z");
        jwtService.submitPerson(p, jws);

        GTAAPerson expected = new GTAAPerson();
        expected.setFamilyName("de Mul");
        expected.setGivenName("Sjaak");
        verify(gtaa).submit(eq(expected), eq("backend:user z"));
    }

    /**
     * Used for testing
     *
     * @return a JWT string
     */

    protected static String encrypt(String issuer, String key, String user) {
        String compactJws = Jwts.builder()
            .setSubject("GTAAPerson")
            .setHeaderParam("iss", issuer)
            .setHeaderParam("usr", user)
            .signWith(SignatureAlgorithm.HS512, key.getBytes())
            .compact();
        log.debug(compactJws);
        return compactJws;
    }

}
