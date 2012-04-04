/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;


import nl.vpro.api.domain.media.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * User: rico
 * Date: 04/04/2012
 */
public class JsonDeserializeToDomainTest {

    @Test
    public void testDeserializeProgram() {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("program.json");
        ObjectMapper mapper = new ObjectMapper();

        try {
            Program program = mapper.readValue(is, Program.class);

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
            assertEquals("De fiets is niet niets...", d.getValue());
            assertEquals(TextualType.MAIN, d.getType());

            Title t = program.getTitles().iterator().next();
            assertEquals("De Avonden aflevering 1 ", t.getValue());
            assertEquals(OwnerType.BROADCASTER, t.getOwner());
            assertEquals(TextualType.MAIN, t.getType());

            Location l = program.getLocations().iterator().next();
            assertEquals("urn:vpro:media:location:13255638", l.getUrn());
            assertEquals(1332151833956L, l.getCreationDate().getTime());
            assertEquals(1332151833973L, l.getLastModified().getTime());
            assertEquals("http://s1.fans.ge/mp3/201108/30/Lil_Wayne_ft_Bruno_Mars_-_Mirror_2011(fans_ge).mp3", l.getProgramUrl());
            assertEquals(Workflow.PUBLISHED, l.getWorkflow());
            assertEquals(LocationType.INTERNAL, l.getType());
            AvAttributes ava = l.getAvAttributes();
            assertEquals(AvFileFormat.MP3, ava.getAvFileFormat());

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
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDeserializeGroup() {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("group.json");
        ObjectMapper mapper = new ObjectMapper();

        try {
            Group group = mapper.readValue(is, Group.class);

            assertNotNull(group);

            assertEquals(1332764836919L, group.getLastModified().getTime());
            assertEquals(1, group.getGenres().size());
            assertEquals("Kunst/Cultuur", group.getGenres().iterator().next());
            assertTrue(group.isEmbeddable());


            assertEquals(3, group.getScheduleEvents().size());
            assertEquals(1, group.getDescriptions().size());
            assertEquals(3, group.getTitles().size());
            assertEquals(1, group.getLocations().size());
            assertEquals(2, group.getImages().size());
            assertEquals(2, group.getDescendantOf().size());

            ScheduleEvent se = group.getScheduleEvents().iterator().next();
            assertEquals(10800000L, se.getDuration().getTime());
            assertEquals(1266364920000L, se.getStart().getTime());
            assertEquals("urn:vpro:media:program:6685", se.getUrnRef());
            assertEquals(Channel.RAD5, se.getChannel());
            assertEquals("imi:24653", se.getImi());

            assertEquals(true, se.getRepeat().isRerun());
            assertEquals("fietsbel", se.getRepeat().getValue());

            Description d = group.getDescriptions().iterator().next();
            assertEquals(OwnerType.BROADCASTER, d.getOwner());
            assertEquals("De fiets is niet niets...", d.getValue());
            assertEquals(TextualType.MAIN, d.getType());

            Title t = group.getTitles().iterator().next();
            assertEquals("De Avonden aflevering 1 ", t.getValue());
            assertEquals(OwnerType.BROADCASTER, t.getOwner());
            assertEquals(TextualType.MAIN, t.getType());

            Location l = group.getLocations().iterator().next();
            assertEquals("urn:vpro:media:location:13255638", l.getUrn());
            assertEquals(1332151833956L, l.getCreationDate().getTime());
            assertEquals(1332151833973L, l.getLastModified().getTime());
            assertEquals("http://s1.fans.ge/mp3/201108/30/Lil_Wayne_ft_Bruno_Mars_-_Mirror_2011(fans_ge).mp3", l.getProgramUrl());
            assertEquals(Workflow.PUBLISHED, l.getWorkflow());
            assertEquals(LocationType.INTERNAL, l.getType());
            AvAttributes ava = l.getAvAttributes();
            assertEquals(AvFileFormat.MP3, ava.getAvFileFormat());

            Image i = group.getImages().iterator().next();
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

            DescendantRef dr = group.getDescendantOf().iterator().next();
            assertEquals("urn:vpro:media:group:11887617", dr.getUrnRef());


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
