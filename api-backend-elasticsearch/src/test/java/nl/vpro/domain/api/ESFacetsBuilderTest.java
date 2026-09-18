package nl.vpro.domain.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ESFacetsBuilderTest {

    @Test
    void escapeFacetName() {
        assertThat(ESFacetsBuilder.escape("Eerste componist")).isEqualTo("Eerste/componist");
    }
/*
    @Test(expected = UnsupportedOperationException.class)
    void year() {
        assertThat(IntervalUnit.YEAR.getShortEs());
    }*/
 /*   @Test
    public void years() {
        assertThat(ESFacetsBuilder.ESInterval.parse("YEAR").number).isEqualTo(1);
        assertThat(ESFacetsBuilder.ESInterval.parse("2YEAR").unit).isEqualTo(ESFacetsBuilder.IntervalUnit.YEAR);
        assertThat(ESFacetsBuilder.ESInterval.parse("YEAR").getEsValue()).isEqualTo("year");
    }*/
    /*
    @Test(expected = UnsupportedOperationException.class)
    void month() {
        assertThat(IntervalUnit.MONTH.getShortEs());
    }*/
/*
    @Test
    void months() {
        assertThat(ESFacetsBuilder.ESInterval.parse("MONTH").number).isEqualTo(1);
        assertThat(ESFacetsBuilder.ESInterval.parse("MONTH").unit).isEqualTo(ESFacetsBuilder.IntervalUnit.MONTH);
        assertThat(ESFacetsBuilder.ESInterval.parse("MONTH").getEsValue()).isEqualTo("month");


    }

    @Test
    void weeks() {
        assertThat(ESFacetsBuilder.ESInterval.parse("2WEEK").number).isEqualTo(2);
        assertThat(ESFacetsBuilder.ESInterval.parse("2WEEK").unit).isEqualTo(ESFacetsBuilder.IntervalUnit.WEEK);
        assertThat(ESFacetsBuilder.ESInterval.parse("2WEEK").getEsValue()).isEqualTo("2w");
    }


    @Test
    void days() {
        assertThat(ESFacetsBuilder.ESInterval.parse("2DAY").number).isEqualTo(2);
        assertThat(ESFacetsBuilder.ESInterval.parse("2DAY").unit).isEqualTo(ESFacetsBuilder.IntervalUnit.DAY);
        assertThat(ESFacetsBuilder.ESInterval.parse("2DAY").getEsValue()).isEqualTo("2d");
    }

    @Test
    void hours() {
        assertThat(ESFacetsBuilder.ESInterval.parse("2HOUR").number).isEqualTo(2);
        assertThat(ESFacetsBuilder.ESInterval.parse("2HOUR").unit).isEqualTo(ESFacetsBuilder.IntervalUnit.HOUR);
        assertThat(ESFacetsBuilder.ESInterval.parse("2HOUR").getEsValue()).isEqualTo("2h");


    }

    @Test
    void minutes() {
        assertThat(ESFacetsBuilder.ESInterval.parse("2MINUTE").number).isEqualTo(2);
        assertThat(ESFacetsBuilder.ESInterval.parse("2MINUTE").unit).isEqualTo(ESFacetsBuilder.IntervalUnit.MINUTE);
        assertThat(ESFacetsBuilder.ESInterval.parse("2MINUTE").getEsValue()).isEqualTo("2m");


    }*/
}
