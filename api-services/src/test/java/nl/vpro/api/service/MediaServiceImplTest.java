package nl.vpro.api.service;

import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.api.transfer.MediaSearchResultItem;

import static org.junit.Assert.assertTrue;


/**
 * @author Michiel Meeuwissen
 * @since 1.8
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/nl/vpro/api/service/mediaservice-context.xml"})
public class MediaServiceImplTest {

    @Autowired
    MediaServiceImpl impl;

    @Test
    public void testGetProfile() throws Exception {

        Iterator<MediaObject> result = impl.getProfile("woord");
        assertTrue(result.hasNext());


    }
}
