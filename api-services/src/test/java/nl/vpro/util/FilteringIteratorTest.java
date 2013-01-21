package nl.vpro.util;

import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.api.service.Profile;
import nl.vpro.api.service.search.fiterbuilder.SearchFilter;
import nl.vpro.api.util.CouchdbViewIterator;
import nl.vpro.api.util.MediaObjectIterator;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static org.junit.Assert.assertNotNull;

/**
 * @author Michiel Meeuwissen
 * @since 1.8
 */
public class FilteringIteratorTest {

    @Test
    public void test() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("exampleview.json");
        assertNotNull(inputStream);

        SearchFilter filter = Profile.WOORD.createFilterQuery();
        Iterator<MediaObject> iterator =
            new FilteringIterator<MediaObject>(new MediaObjectIterator(new CouchdbViewIterator(inputStream)), filter);
        while(iterator.hasNext()) {
            System.out.println(iterator.next());
        }

    }

}
