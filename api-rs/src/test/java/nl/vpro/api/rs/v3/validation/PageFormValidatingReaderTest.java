package nl.vpro.api.rs.v3.validation;

import java.io.ByteArrayInputStream;

import javax.xml.bind.UnmarshalException;

import org.junit.Test;

import nl.vpro.domain.api.page.PageForm;

public class PageFormValidatingReaderTest {

    @Test(expected = UnmarshalException.class)
    public void testReadFromInvalid() throws Exception {
        PageFormValidatingReader reader = new PageFormValidatingReader();

        byte[] bytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<pagesForm xmlns=\"urn:vpro:api:2013\" xmlns:media=\"urn:vpro:media:2009\" highlight=\"false\">\n" +
            "  <searches>\n" +
            "    <sortDatee>\n" +
            "      <begin>2009-01-01T00:00:00+01:00</begin>\n" +
            "      <end>2010-01-01T00:00:00+01:00</end>\n" +
            "    </sortDatee>\n" +
            "  </searches>\n" +
            "  <sortFields>\n" +
            "    <sort order=\"ASC\">sortDate</sort>\n" +
            "  </sortFields>\n" +
            "  <facets>\n" +
            "    <sortDates>\n" +
            "      <interval>YEAR</interval>\n" +
            "    </sortDates>\n" +
            "  </facets>\n" +
            "</pagesForm>\n").getBytes();
        reader.unmarshal(new ByteArrayInputStream(bytes));
    }

    @Test
    public void testRead() throws Exception {
        PageFormValidatingReader reader = new PageFormValidatingReader();

        byte[] bytes = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<pagesForm xmlns=\"urn:vpro:api:2013\" xmlns:media=\"urn:vpro:media:2009\" highlight=\"false\">\n" +
            "  <searches>\n" +
            "    <sortDates>\n" +
            "      <matcher>" +
            "      <begin>2009-01-01T00:00:00+01:00</begin>\n" +
            "      <end>2010-01-01T00:00:00+01:00</end>\n" +
            "      </matcher>" +
            "    </sortDates>\n" +
            "  </searches>\n" +
            "  <sortFields>\n" +
            "    <sort order=\"ASC\">sortDate</sort>\n" +
            "  </sortFields>\n" +
            "  <facets>\n" +
            "    <sortDates>\n" +
            "      <interval>YEAR</interval>\n" +
            "    </sortDates>\n" +
            "  </facets>\n" +
            "</pagesForm>\n").getBytes();
        PageForm form = reader.unmarshal(new ByteArrayInputStream(bytes));
    }
}
