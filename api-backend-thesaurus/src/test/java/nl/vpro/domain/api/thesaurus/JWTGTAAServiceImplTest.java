package nl.vpro.domain.api.thesaurus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import nl.vpro.rs.thesaurus.update.NewPerson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import nl.vpro.beeldengeluid.gtaa.GTAARepository;
import nl.vpro.domain.media.gtaa.GTAAPerson;

/**
 * Wraps the GTAAService allowing {@link GTAAPerson}s to be submitted using JWT
 *
 * @author machiel
 * @since 5.4
 */
@RunWith(MockitoJUnitRunner.class)
public class JWTGTAAServiceImplTest {

    @Mock
    GTAARepository gtaa;

    @Mock
    GTAAKeysRepository keysRepo;

    @InjectMocks
    JWTGTAAServiceImpl jwtService = new JWTGTAAServiceImpl();

    @Test
    public void submitPersonShouldCallSubmitPersonOnGTAAMock() throws IOException {
        when(keysRepo.getKeyFor("backend-1")).thenReturn(Optional.of("SECRETABC"));

        NewPerson p = new NewPerson();
        String jws = jwtService.encrypt("backend-1", "SECRETABC", "user x");
        jwtService.submitPerson(p, jws);

        verify(gtaa).submit(any(GTAAPerson.class), eq("backend-1:user x"));

    }

    @Test
    public void testGtaaUri() throws IOException {
        when(keysRepo.getKeyFor("backend-2")).thenReturn(Optional.of("SECRETCBA"));

        NewPerson p = new NewPerson();
        String jws = jwtService.encrypt("backend-2", "SECRETCBA", "user y");
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

        String jws = jwtService.encrypt("backend", "SECRET", "user z");
        jwtService.submitPerson(p, jws);

        GTAAPerson expected = new GTAAPerson();
        expected.setFamilyName("de Mul");
        expected.setGivenName("Sjaak");
        verify(gtaa).submit(eq(expected), eq("backend:user z"));
    }

}
