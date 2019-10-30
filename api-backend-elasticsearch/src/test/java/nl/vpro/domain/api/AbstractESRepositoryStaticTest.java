package nl.vpro.domain.api;

import org.junit.jupiter.api.Test;

import nl.vpro.domain.media.MediaBuilder;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.support.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michiel Meeuwissen
 * @since 3.9
 */
public class AbstractESRepositoryStaticTest {

    @Test
    public void testHasESPath() {
        Program program = MediaBuilder.program().mid("POMS_123").titles(new Title("bla", OwnerType.BROADCASTER, TextualType.MAIN)).build();
        assertThat(AbstractESRepository.hasEsPath(program, "titles.value")).isTrue();
        assertThat(AbstractESRepository.hasEsPath(program, "descriptions.value")).isFalse();
        assertThat(AbstractESRepository.hasEsPath(program, "mid")).isTrue();
    }

    @Test
    public void tesFilterFields() {
        Program program = MediaBuilder.program().mid("POMS_123").tags(new Tag("bla")).titles(new Title("bla", OwnerType.BROADCASTER, TextualType.MAIN)).build();
        assertThat(AbstractESRepository.filterFields(program, new String[]{"tags", "descriptions.value"}, "titles.value")).containsOnly("tags");
        assertThat(AbstractESRepository.filterFields(program, new String[]{"images.title", "descriptions.value"}, "titles.value")).containsOnly("titles.value");
    }
}
