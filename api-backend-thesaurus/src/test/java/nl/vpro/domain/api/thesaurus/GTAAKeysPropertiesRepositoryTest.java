package nl.vpro.domain.api.thesaurus;

import java.util.NoSuchElementException;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(SpringExtension.class) //  implicitly uses GTAAKeysPropertiesRepositoryTest-context.xml ?
public class GTAAKeysPropertiesRepositoryTest {

    @Inject
    GTAAKeysRepositoryImpl repo;

    @Test
    public void testGetKeyFor1() {
        assertThat(repo.getKeyFor("npo-functional-tests").get()).isEqualTo("***REMOVED***");
    }

    @Test
    @Disabled
    public void testGetKeyFor2() {
        assertThat(repo.getKeyFor("issuer").get()).isEqualTo("test");
    }

    @Test
    public void testGetKeyFor3() {
        assertThatThrownBy(() -> {
            repo.getKeyFor("demo-user").get();
        }).isInstanceOf(NoSuchElementException.class);
    }

}
