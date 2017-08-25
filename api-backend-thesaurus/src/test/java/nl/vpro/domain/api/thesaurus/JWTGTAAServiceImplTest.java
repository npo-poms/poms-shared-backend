package nl.vpro.domain.api.thesaurus;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import nl.vpro.beeldengeluid.gtaa.GTAARepository;
import nl.vpro.domain.api.thesaurus.GTAAKeysRepository;
import nl.vpro.domain.api.thesaurus.JWTGTAAServiceImpl;
import nl.vpro.domain.media.gtaa.GTAAPerson;
import nl.vpro.domain.media.gtaa.GTAARecord;
import nl.vpro.domain.media.gtaa.Status;

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
    public void testGtaaRecord() throws IOException {
        when(keysRepo.getKeyFor("backend-1")).thenReturn(Optional.of("SECRETABC"));

        GTAAPerson p = new GTAAPerson();
        p.setGtaaRecord(new GTAARecord("http://data.beeldengeluid.nl/api/collections/beng:gtaa/1672211",
                Status.candidate, false));

        String jws = jwtService.encryptPerson(p, "backend-1", "SECRETABC", "user x");
        jwtService.submitPerson(jws);

        verify(gtaa).submit(p, "backend-1:user x");
    }

    @Test
    public void testGtaaUri() throws IOException {
        when(keysRepo.getKeyFor("backend-2")).thenReturn(Optional.of("SECRETCBA"));

        GTAAPerson p = new GTAAPerson();
        p.setGtaaUri("http://data.beeldengeluid.nl/api/collections/beng:gtaa/1672211");

        String jws = jwtService.encryptPerson(p, "backend-2", "SECRETCBA", "user y");
        jwtService.submitPerson(jws);

        verify(gtaa).submit(p, "backend-2:user y");
    }

    @Test
    public void testNames() throws IOException {
        when(keysRepo.getKeyFor("backend")).thenReturn(Optional.of("SECRET"));

        GTAAPerson p = new GTAAPerson();
        p.setGivenName("Sjaak");
        p.setFamilyName("de Mul");

        String jws = jwtService.encryptPerson(p, "backend", "SECRET", "user z");
        jwtService.submitPerson(jws);
        System.out.println(jws);
        verify(gtaa).submit(p, "backend:user z");
    }

}
