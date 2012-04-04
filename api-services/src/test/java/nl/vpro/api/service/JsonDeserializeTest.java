package nl.vpro.api.service;

import nl.vpro.jackson.DurationDeserializer;
import nl.vpro.jackson.MediaMapper;
import nl.vpro.jackson.ProgramProblemHandler;
import nl.vpro.domain.image.ImageType;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.*;
import nl.vpro.jackson.RepeatDeserializer;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.DateDeserializer;
import org.codehaus.jackson.map.ext.CoreXMLDeserializers;
import org.codehaus.jackson.map.module.SimpleModule;
import org.junit.Test;

import javax.xml.datatype.Duration;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Date: 26-3-12
 * Time: 15:37
 *
 * @author Ernst Bunders
 */
public class JsonDeserializeTest {

    @Test
    public void testDeserializerProgram() {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("program.json");
        ObjectMapper mapper = new ObjectMapper();

        Module module = new SimpleModule("PomsModule", new Version(1,0,0, null))
          .addDeserializer(Repeat.class, new RepeatDeserializer());

        mapper.registerModule(module);

        DeserializationConfig deserializationConfig =  mapper.getDeserializationConfig();
        deserializationConfig.set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        deserializationConfig.addHandler(new ProgramProblemHandler());
        try {
            Program program = mapper.readValue(is,Program.class);

            assertNotNull(program);

            assertEquals(1332764836919L, program.getLastModified().getTime());
            assertEquals(1, program.getGenres().size());
            assertEquals("Kunst/Cultuur", program.getGenres().iterator().next());
            assertTrue(program.isEmbeddable());


            assertEquals(3, program.getScheduleEvents().size());
            assertEquals(1, program.getDescriptions().size());
            assertEquals(3, program.getTitles().size());
            assertEquals(1, program.getLocations().size());
            assertEquals(2, program.getImages().size());
            assertEquals(2, program.getDescendantOf().size());

            ScheduleEvent se = program.getScheduleEvents().iterator().next();
            assertEquals(10800000L, se.getDuration().getTime());
            assertEquals(1266364920000L, se.getStart().getTime());
            assertEquals("urn:vpro:media:program:6685", se.getUrnRef());
            assertEquals(Channel.RAD5, se.getChannel());
            assertEquals("imi:24653", se.getImi());

            assertEquals(true, se.getRepeat().isRerun());
            assertEquals("fietsbel", se.getRepeat().getValue());

            Description d = program.getDescriptions().iterator().next();
            assertEquals(OwnerType.BROADCASTER, d.getOwner());
            assertEquals("De fiets is niet niets...", d.getDescription());
            assertEquals(TextualType.MAIN, d.getType());

            Title t = program.getTitles().iterator().next();
            assertEquals("De Avonden aflevering 1 ", t.getTitle());
            assertEquals(OwnerType.BROADCASTER, t.getOwner());
            assertEquals(TextualType.MAIN, t.getType());

            Location l = program.getLocations().iterator().next();
            assertEquals("urn:vpro:media:location:13255638", l.getUrn());
            assertEquals(1332151833956L, l.getCreationDate().getTime());
            assertEquals(1332151833973L, l.getLastModified().getTime());
            assertEquals("http://s1.fans.ge/mp3/201108/30/Lil_Wayne_ft_Bruno_Mars_-_Mirror_2011(fans_ge).mp3", l.getProgramUrl());
            assertEquals(Workflow.PUBLISHED, l.getWorkflow());
            assertEquals(LocationType.INTERNAL, l.getType());
            AVAttributes ava = l.getAvAttributes();
            assertEquals(AVFileFormat.MP3, ava.getAvFileFormat());

            Image i = program.getImages().iterator().next();
            assertEquals("urn:vpro:image:55779", i.getImageUri());
            assertEquals("urn:vpro:media:image:13255959", i.getUrn());
            assertEquals(1332174048776L, i.getCreationDate().getTime());
            assertEquals(1332174048791L, i.getLastModified().getTime());
            assertEquals("afbeelding 1", i.getTitle());
            assertEquals(new Integer(183), i.getHeight());
            assertEquals(new Integer(183), i.getWidth());
            assertEquals("Test afbeelding een", i.getDescription());
            assertEquals(ImageType.PORTRAIT, i.getType());
            assertEquals(Workflow.PUBLISHED, i.getWorkflow());

            DescendantRef dr = program.getDescendantOf().iterator().next();
            assertEquals("urn:vpro:media:group:11887617", dr.getUrnRef());




        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
