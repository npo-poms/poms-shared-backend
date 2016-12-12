/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXB;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.Segment;
import nl.vpro.domain.media.exceptions.ModificationException;
import nl.vpro.test.util.jackson2.Jackson2TestUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class ApiMediaFilterTest {

    @BeforeClass
    public static void init() {
        MediaPropertiesFilters.instrument();
    }

    @Before()
    public void setUp() {
        ApiMediaFilter.removeFilter();
    }

    @Test
    public void testFilterObjectField() throws Exception {
        Program program = MediaTestDataBuilder.program().withSource().build();

        ApiMediaFilter.set("source");
        assertThat(program.getSource()).isNotNull();

        ApiMediaFilter.set("titles");
        assertThat(program.getSource()).isNull();
    }

    @Test
    public void testJaxbReadWrite() throws Exception {
        ApiMediaFilter.set("titles");

        final Program program = JAXB.unmarshal(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<program embeddable=\"true\" sortDate=\"1970-01-01T01:00:00.100+01:00\" creationDate=\"2014-02-18T12:13:50.123+01:00\" workflow=\"FOR PUBLICATION\" urn=\"urn:vpro:media:program:null\" xmlns=\"urn:vpro:media:2009\" xmlns:shared=\"urn:vpro:shared:2009\">\n" +
                "    <scheduleEvents>\n" +
                "        <scheduleEvent channel=\"NED3\" urnRef=\"urn:vpro:media:program:null\">\n" +
                "            <start>1970-01-01T01:00:00.100+01:00</start>\n" +
                "            <duration>P0DT0H0M0.200S</duration>\n" +
                "        </scheduleEvent>\n" +
                "        <scheduleEvent channel=\"NED3\" net=\"ZAPP\" urnRef=\"urn:vpro:media:program:null\">\n" +
                "            <start>1970-01-04T01:00:00.300+01:00</start>\n" +
                "            <duration>P0DT0H0M0.050S</duration>\n" +
                "        </scheduleEvent>\n" +
                "    </scheduleEvents>\n" +
                "</program>"), Program.class);

        assertThat(program.getScheduleEvents()).isEmpty();

        Writer writer = new StringWriter();
        JAXB.marshal(program, writer);

        assertThat(writer.toString()).doesNotContain("<scheduleEvent ");
    }

    @Test
    public void testFilterSegments() {
        Program program = MediaTestDataBuilder.program().mid("MID_TEST").withSegments().build();
        assertThat(program.getSegments()).hasSize(3);
        assertThat(program.getSegments().first().getMidRef()).isNotNull();

        /* Singular */
        ApiMediaFilter.set("segment");
        assertThat(program.getSegments()).hasSize(1);
        Segment segment = program.getSegments().first();
        assertThat(segment.getMidRef()).isNotNull();

        ApiMediaFilter.set("segment:2");
        assertThat(program.getSegments()).hasSize(2);

        ApiMediaFilter.set("segment:1");
        assertThat(program.getSegments()).hasSize(1);

        ApiMediaFilter.set("segment:0");
        assertThat(program.getSegments()).isEmpty();

        /* Plural */
        ApiMediaFilter.set("segments");
        assertThat(program.getSegments()).hasSize(3);

        ApiMediaFilter.set("segments:2");
        assertThat(program.getSegments()).hasSize(2);

        ApiMediaFilter.set("segments:1");
        assertThat(program.getSegments()).hasSize(1);

        ApiMediaFilter.set("segments:0");
        assertThat(program.getSegments()).isEmpty();
    }

    @Test
    public void testFilterType() {
        Program program = MediaTestDataBuilder.program().withType().build();
        assertThat(program.getType()).isNotNull();

        ApiMediaFilter.set("type");
        assertThat(new FilteredObject<>("type", program.getType()).value()).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFilterUnknownProperty() {
        try {
            ApiMediaFilter.set("bestaatniet");
        } catch (IllegalArgumentException ia) {
            assertThat(ia.getMessage()).startsWith("The property bestaatniet is not known.");
            throw ia;
        }

    }

    @Test
    public void testFilterAwards() throws ModificationException {
        Program program = MediaTestDataBuilder.program().awards("AWARD").build();
        assertThat(program.getAwards()).isNotNull();

        /* Should be null */
        ApiMediaFilter.set("title");
        assertThat(new FilteredObject<>("awards", program.getAwards()).value()).isNull();

        /* Singular */
        ApiMediaFilter.set("award");
        assertThat(new FilteredObject<>("award", program.getAwards()).value()).isNotNull();

        ApiMediaFilter.set("award:1");
        assertThat(new FilteredObject<>("award", program.getAwards()).value()).isNotNull();

        ApiMediaFilter.set("award:10");
        assertThat(new FilteredObject<>("award", program.getAwards()).value()).isNotNull();

        /* Plural */
        ApiMediaFilter.set("awards");
        assertThat(new FilteredObject<>("awards", program.getAwards()).value()).isNotNull();

        ApiMediaFilter.set("awards:1");
        assertThat(new FilteredObject<>("awards", program.getAwards()).value()).isNotNull();

        ApiMediaFilter.set("awards:10");
        assertThat(new FilteredObject<>("awards", program.getAwards()).value()).isNotNull();
    }

    @Test
    public void testFilterCrids() throws ModificationException {
        Program program = MediaTestDataBuilder.program().crids("crid1", "crid2", "crid3").build();
        assertThat(program.getCrids()).isNotNull();
        assertThat(program.getCrids()).hasSize(3);

        /* Singular */
        ApiMediaFilter.set("crid");
        assertThat(FilteredList.wrap("crids", program.getCrids())).hasSize(1);

        ApiMediaFilter.set("crid:1");
        assertThat(FilteredList.wrap("crids", program.getCrids())).hasSize(1);

        ApiMediaFilter.set("crid:10");
        assertThat(FilteredList.wrap("crids", program.getCrids())).hasSize(3);

        /* Plural */
        ApiMediaFilter.set("crids");
        assertThat(FilteredList.wrap("crids", program.getCrids())).hasSize(3);

        ApiMediaFilter.set("crids:1");
        assertThat(FilteredList.wrap("crids", program.getCrids())).hasSize(1);

        ApiMediaFilter.set("crids:10");
        assertThat(FilteredList.wrap("crids", program.getCrids())).hasSize(3);
    }

    @Test
    public void testSegment() throws Exception {
        Segment segment = Jackson2TestUtil.roundTrip(
                MediaTestDataBuilder.segment()
                        .midRef("ABC_DEF")
                        .withLocations()
                        .build());
        assertThat(segment.getLocations()).hasSize(4);
        ApiMediaFilter.set("location");

        assertThat(segment.getLocations()).hasSize(1);

        assertThat(segment.getMidRef()).isEqualTo("ABC_DEF");
    }
}
