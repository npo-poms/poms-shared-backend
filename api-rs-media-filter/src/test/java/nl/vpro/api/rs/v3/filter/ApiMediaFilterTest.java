/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.exceptions.ModificationException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

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

        ApiMediaFilter.get().filter("source");
        assertThat(program.getSource()).isNotNull();

        ApiMediaFilter.get().filter("titles");
        assertThat(program.getSource()).isNull();
    }

    @Test
    public void testJaxbReadWrite() throws Exception {
        ApiMediaFilter.get().filter("titles");

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
        Program program = MediaTestDataBuilder.program().withSegments().build();
        assertThat(program.getSegments()).hasSize(3);

        /* Singular */
        ApiMediaFilter.get().filter("segment");
        assertThat(new FilteredSortedSet<>("segment", program.getSegments())).hasSize(1);

        ApiMediaFilter.get().filter("segment:2");
        assertThat(new FilteredSortedSet<>("segment", program.getSegments())).hasSize(2);

        ApiMediaFilter.get().filter("segment:1");
        assertThat(new FilteredSortedSet<>("segment", program.getSegments())).hasSize(1);

        ApiMediaFilter.get().filter("segment:0");
        assertThat(new FilteredSortedSet<>("segment", program.getSegments())).isEmpty();

        /* Plural */
        ApiMediaFilter.get().filter("segments");
        assertThat(new FilteredSortedSet<>("segments", program.getSegments())).hasSize(3);

        ApiMediaFilter.get().filter("segments:2");
        assertThat(new FilteredSortedSet<>("segments", program.getSegments())).hasSize(2);

        ApiMediaFilter.get().filter("segments:1");
        assertThat(new FilteredSortedSet<>("segments", program.getSegments())).hasSize(1);

        ApiMediaFilter.get().filter("segments:0");
        assertThat(new FilteredSortedSet<>("segments", program.getSegments())).isEmpty();
    }

    @Test
    public void testFilterType() {
        Program program = MediaTestDataBuilder.program().withType().build();
        assertThat(program.getType()).isNotNull();

        ApiMediaFilter.get().filter("type");
        assertThat(new FilteredObject<>("type", program.getType()).value()).isNotNull();
    }

    @Test
    public void testFilterAwards() throws ModificationException {
        Program program = MediaTestDataBuilder.program().awards("AWARD").build();
        assertThat(program.getAwards()).isNotNull();

        /* Should be null */
        ApiMediaFilter.get().filter("title");
        assertThat(new FilteredObject<>("awards", program.getAwards()).value()).isNull();

        /* Singular */
        ApiMediaFilter.get().filter("award");
        assertThat(new FilteredObject<>("award", program.getAwards()).value()).isNotNull();

        ApiMediaFilter.get().filter("award:1");
        assertThat(new FilteredObject<>("award", program.getAwards()).value()).isNotNull();

        ApiMediaFilter.get().filter("award:10");
        assertThat(new FilteredObject<>("award", program.getAwards()).value()).isNotNull();

        /* Plural */
        ApiMediaFilter.get().filter("awards");
        assertThat(new FilteredObject<>("awards", program.getAwards()).value()).isNotNull();

        ApiMediaFilter.get().filter("awards:1");
        assertThat(new FilteredObject<>("awards", program.getAwards()).value()).isNotNull();

        ApiMediaFilter.get().filter("awards:10");
        assertThat(new FilteredObject<>("awards", program.getAwards()).value()).isNotNull();
    }

    @Test
    public void testFilterCrids() throws ModificationException {
        Program program = MediaTestDataBuilder.program().crids("crid1", "crid2", "crid3").build();
        assertThat(program.getCrids()).isNotNull();
        assertThat(program.getCrids()).hasSize(3);

        /* Singular */
        ApiMediaFilter.get().filter("crid");
        assertThat(new FilteredList<>("crid", program.getCrids())).hasSize(1);

        ApiMediaFilter.get().filter("crid:1");
        assertThat(new FilteredList<>("crid", program.getCrids())).hasSize(1);

        ApiMediaFilter.get().filter("crid:10");
        assertThat(new FilteredList<>("crid", program.getCrids())).hasSize(3);

        /* Plural */
        ApiMediaFilter.get().filter("crids");
        assertThat(new FilteredList<>("crids", program.getCrids())).hasSize(3);

        ApiMediaFilter.get().filter("crids:1");
        assertThat(new FilteredList<>("crids", program.getCrids())).hasSize(1);

        ApiMediaFilter.get().filter("crids:10");
        assertThat(new FilteredList<>("crids", program.getCrids())).hasSize(3);
    }
}
