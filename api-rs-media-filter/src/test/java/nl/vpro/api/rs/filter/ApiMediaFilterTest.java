/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.filter;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import javax.xml.bind.JAXB;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.meeuw.functional.Suppliers;

import nl.vpro.domain.bind.AbstractJsonIterable;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.Workflow;
import nl.vpro.test.util.jackson2.Jackson2TestUtil;

import static nl.vpro.test.util.jackson2.Jackson2TestUtil.assertThatJson;
import static nl.vpro.jassert.assertions.MediaAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Log4j2
public class ApiMediaFilterTest {
    static {
        MediaPropertiesFilters.instrument();
    }


    @BeforeEach
    public void setUp() {
        ApiMediaFilter.removeFilter();
    }
    @AfterEach
    public void shutdown() {
        ApiMediaFilter.removeFilter();
        AbstractJsonIterable.DEFAULT_CONSIDER_JSON_INCLUDE.remove();
    }

    @Test
    public void testFilterObjectField() {
        Program program = MediaTestDataBuilder.program()
            .withMemberOf()
            .withSource().build();

        ApiMediaFilter.set("source");
        assertThat(program.getSource()).isNotNull();

        ApiMediaFilter.set("titles");
        assertThat(program.getSource()).isNull();

        ApiMediaFilter.removeFilter();
        assertThat(program.getMemberOf().first().getType()).isEqualTo(MediaType.SEASON);

    }

    @Test
    public void testJaxbReadWrite() {
        ApiMediaFilter.set("titles");

        final Program program = JAXB.unmarshal(new StringReader("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <program embeddable="true" sortDate="1970-01-01T01:00:00.100+01:00" creationDate="2014-02-18T12:13:50.123+01:00" workflow="FOR PUBLICATION" urn="urn:vpro:media:program:null" xmlns="urn:vpro:media:2009" xmlns:shared="urn:vpro:shared:2009">
                <scheduleEvents>
                    <scheduleEvent channel="NED3" urnRef="urn:vpro:media:program:null">
                        <start>1970-01-01T01:00:00.100+01:00</start>
                        <duration>P0DT0H0M0.200S</duration>
                    </scheduleEvent>
                    <scheduleEvent channel="NED3" net="ZAPP" urnRef="urn:vpro:media:program:null">
                        <start>1970-01-04T01:00:00.300+01:00</start>
                        <duration>P0DT0H0M0.050S</duration>
                    </scheduleEvent>
                </scheduleEvents>
            </program>"""), Program.class);

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
        assertThat(program.getType()).isNotNull();

        ApiMediaFilter.set("title");
        assertThat(new FilteredObject<>("type", program.getType()).value()).isNull();
        //assertThat(program.getType()).isNull(); // 'type' is excludes, so wont be filtered implicitely.
    }

    @Test
    public void testFilterPredictions() {
        Program program = MediaTestDataBuilder.program().withPredictions().build();

        assertThat(program.getPredictions()).isNotNull();

        ApiMediaFilter.set("predictions");
        assertThat(new FilteredObject<>("prediction", program.getPredictions()).value()).isNotNull();
        assertThat(program.getPredictions()).isNotNull();
        assertThat(program.getPredictions()).hasSize(2);


        ApiMediaFilter.set("prediction");
        assertThat(new FilteredObject<>("prediction", program.getPredictions()).value()).isNotNull();
        assertThat(program.getPredictions()).isNotNull();
        assertThat(program.getPredictions()).hasSize(1);

        ApiMediaFilter.set("title");
        assertThat(new FilteredObject<>("prediction", program.getPredictions()).value()).isNull();
        assertThat(program.getPredictions()).isEmpty();
    }


    @Test
    public void testFilterUnknownProperty() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ApiMediaFilter.set("bestaatniet"))
                .withMessageStartingWith("Unrecognized properties [bestaatniet]");
    }

    @Test
    public void testFilterAwards() {
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
    public void testFilterCrids() {
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
                .mid("segment_mid")
                .withLocations()
                .build());
        assertThat(segment.getLocations()).hasSize(4);
        assertThat(segment.getLocations().first().getWorkflow()).isEqualTo(Workflow.PUBLISHED);
        ApiMediaFilter.set("location,workflow");

        assertThat(segment.getLocations()).hasSize(1);
        Location location = segment.getLocations().first();
        Workflow wf = location.getWorkflow();
        assertThat(wf).isEqualTo(Workflow.PUBLISHED); // it is a bit odd that you have to request 'workflow' to also receive the workflow of location
        assertThat(location.getProgramUrl()).isEqualTo("http://cgi.omroep.nl/legacy/nebo?/ceres/1/vpro/rest/2009/VPRO_1132492/bb.20090317.m4v");



        assertThat(segment.getMidRef()).isEqualTo("ABC_DEF");
    }

    @Test
    public void testFilterIgnoreFields() {
        Program program = MediaTestDataBuilder.program()
            .mid("MID_123")
            .avType(AVType.MIXED)
            .type(ProgramType.PROMO)
            .creationDate(LocalDateTime.of(2019, 8, 26, 21, 50)).build();
        assertThat(program.getSortInstant()).isEqualTo("2019-08-26T19:50:00Z");


        ApiMediaFilter.set("title");
        assertThat(program.getSortInstant()).isEqualTo("2019-08-26T19:50:00Z");
        assertThat(program.getMid()).isEqualTo("MID_123");
        assertThat(program).isMixed();
        assertThat(program.getType()).isEqualTo(ProgramType.PROMO);
        assertThat(program.getPredictions()).isEmpty();


         Segment segment  = MediaTestDataBuilder.segment()
             .midRef("MID_123")
             .mid("MID_124")
             .build();
        assertThat(segment.getMidRef()).isEqualTo("MID_123");


    }


    public static Object createProgram() { // We need to return Object here, otherwise our classes are loaded too early and cannot be instrumented any more by MediaPropertiesFilter
        return MediaTestDataBuilder.program()
            .withFixedDates()
            .mainTitle("foobar")
            .mid("MID_123")
            .withPredictions()
            .workflow(null)
            .build();
    }

    public static Supplier<Object>[] suppliers() {
        return new Supplier[] {
            ApiMediaFilterTest::createProgram,
            Suppliers.memoize(ApiMediaFilterTest::createProgram)
        };
    }

    @ParameterizedTest
    @MethodSource("suppliers")
    public void filterPredictionsTitle(Supplier<Program> program) {
        AbstractJsonIterable.DEFAULT_CONSIDER_JSON_INCLUDE.set(true);

        ApiMediaFilter.set("title");
        assertThat(program.get().getPredictions()).isEmpty();
        assertThatJson(program.get()).isSimilarTo("""
            {
              "objectType" : "program",
              "mid" : "MID_123",
              "sortDate" : 1425596400000,
              "creationDate" : 1425596400000,
              "lastModified" : 1425600000000,
              "embeddable" : true,
              "titles" : [ {
                "value" : "foobar",
                "owner" : "BROADCASTER",
                "type" : "MAIN"
              } ],
              "publishDate" : 1425603600000
            }""");
    }

    @ParameterizedTest
    @MethodSource("suppliers")
    public void filterPredictionsAll(Supplier<Program> program) {
        AbstractJsonIterable.DEFAULT_CONSIDER_JSON_INCLUDE.set(true);
        ApiMediaFilter.set("all");
        Program p = program.get();
        log.info("{}", p);
        assertJsonWithPredictions(p);
    }


    /**
     * See <a href="https://jira.vpro.nl/browse/NPA-602">JIRA</a>
     */
    @ParameterizedTest
    @MethodSource("suppliers")
    public void filterPredictionsTitleAndPredictions(Supplier<Program> program) {
        AbstractJsonIterable.DEFAULT_CONSIDER_JSON_INCLUDE.set(true);
        ApiMediaFilter.set("titles,predictions");

        Program p = program.get();
        assertJsonWithPredictions(p);
    }

    /**
     * See <a href="https://jira.vpro.nl/browse/NPA-602">JIRA</a>
     */
    @ParameterizedTest
    @MethodSource("suppliers")
    public void filterPredictionsTitleAndPredictionsWorkaround(Supplier<Program> program) {
        AbstractJsonIterable.DEFAULT_CONSIDER_JSON_INCLUDE.set(true);
        ApiMediaFilter.set("predictionsForXml,predictions,title");
        Program p = program.get();
        assertJsonWithPredictions(p);
    }

    protected void assertJsonWithPredictions(Object p) { // argument can not be Program, that would break instrumentation

        assertThat(((Program) p).getPredictions()).hasSize(2);
        assertThatJson(p).isSimilarTo("""
            {
              "objectType" : "program",
              "mid" : "MID_123",
              "sortDate" : 1425596400000,
              "creationDate" : 1425596400000,
              "lastModified" : 1425600000000,
              "embeddable" : true,
              "titles" : [ {
                "value" : "foobar",
                "owner" : "BROADCASTER",
                "type" : "MAIN"
              } ],
              "predictions" : [ {
                "state" : "REVOKED",
                "platform" : "INTERNETVOD"
              }, {
                "state" : "ANNOUNCED",
                "platform" : "TVVOD"
              } ],
              "publishDate" : 1425603600000
            }""");

    }


    /**
     * See <a href="https://jira.vpro.nl/browse/NPA-602">JIRA</a>
     */
    @Test
    public void testFilterLocations() {
        AbstractJsonIterable.DEFAULT_CONSIDER_JSON_INCLUDE.set(true);

        Program program = MediaTestDataBuilder.program()
            .mid("MID_123")
            .withFixedDates()
            .locations(Location.builder()
                .programUrl("https://www.vpro.nl")
                .creationDate(Instant.parse("2021-11-05T17:00:00Z"))
                .build()
            )
            .build();

        ApiMediaFilter.set("title");
        assertThat(program.getLocations()).isEmpty();
        assertThatJson(program).isSimilarTo("""
            {
              "objectType" : "program",
              "mid" : "MID_123",
              "sortDate" : 1425596400000,
              "creationDate" : 1425596400000,
              "lastModified" : 1425600000000,
              "embeddable" : true,
              "publishDate" : 1425603600000
            }""");

        ApiMediaFilter.set("all");
        assertThat(program.getLocations()).isNotEmpty();
        assertThatJson(program).isSimilarTo(
            """
                {
                  "objectType" : "program",
                  "mid" : "MID_123",
                  "workflow" : "FOR_PUBLICATION",
                  "sortDate" : 1425596400000,
                  "creationDate" : 1425596400000,
                  "lastModified" : 1425600000000,
                  "embeddable" : true,
                  "locations" : [ {
                    "programUrl" : "https://www.vpro.nl",
                    "avAttributes" : {
                      "avFileFormat" : "UNKNOWN"
                    },
                    "owner" : "BROADCASTER",
                    "creationDate" : 1636131600000,
                    "workflow" : "PUBLISHED"
                  } ],
                  "publishDate" : 1425603600000
                }""");

        ApiMediaFilter.set("title,locations");
        assertThat(program.getLocations()).isNotEmpty();
        assertThatJson(program).isSimilarTo(
            """
                {
                  "objectType" : "program",
                  "mid" : "MID_123",
                  "sortDate" : 1425596400000,
                  "creationDate" : 1425596400000,
                  "lastModified" : 1425600000000,
                  "embeddable" : true,
                  "locations" : [ {
                    "programUrl" : "https://www.vpro.nl",
                    "avAttributes" : {
                      "avFileFormat" : "UNKNOWN"
                    },
                    "owner" : "BROADCASTER",
                    "creationDate" : 1636131600000
                  } ],
                  "publishDate" : 1425603600000
                }""");

 }

}
