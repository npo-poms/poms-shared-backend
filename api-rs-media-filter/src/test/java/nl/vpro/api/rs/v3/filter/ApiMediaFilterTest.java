/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.Program;
import org.junit.Before;
import org.junit.Ignore;
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
@Ignore("Won't run when classes are loaded yet")
public class ApiMediaFilterTest {

    private MediaPropertiesFilters instrument = new MediaPropertiesFilters();

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
}
