package nl.vpro.domain.api.thesaurus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import nl.vpro.beeldengeluid.gtaa.GTAARepository;
import nl.vpro.domain.api.thesaurus.GTAAKeysPropertiesRepository;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class GTAAKeysPropertiesRepositoryTest {

    @Autowired
    GTAAKeysPropertiesRepository repo;
    
    @Test
    public void testGetKeyFor() {
        assertEquals("***REMOVED***", repo.getKeyFor("issuer1").get());
    }

}
