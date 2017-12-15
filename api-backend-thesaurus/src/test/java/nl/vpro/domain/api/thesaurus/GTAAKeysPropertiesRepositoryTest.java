package nl.vpro.domain.api.thesaurus;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class GTAAKeysPropertiesRepositoryTest {

    @Autowired
    GTAAKeysPropertiesRepository repo;

    @Test
    public void testGetKeyFor1() throws IOException {
        assertEquals("***REMOVED***", repo.getKeyFor("npo-functional-tests").get());
    }

    @Test
    @Ignore
    public void testGetKeyFor2() throws IOException {
        assertEquals("test", repo.getKeyFor("issuer").get());
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetKeyFor3() throws IOException {
        assertEquals("test2", repo.getKeyFor("demo-user").get());
    }

}
