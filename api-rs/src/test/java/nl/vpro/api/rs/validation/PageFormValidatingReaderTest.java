package nl.vpro.api.rs.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import nl.vpro.api.util.ApiMappings;
import nl.vpro.domain.api.page.PageForm;

public class PageFormValidatingReaderTest {

    private static final byte[] VALID_FORM = ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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

    @Test(expected = UnmarshalException.class)
    public void testReadFromInvalid() throws Exception {
        PageFormValidatingReader reader = createReader();
        reader.setDoValidate(true);
        reader.init();

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
        PageFormValidatingReader reader = createReader();
        reader.setDoValidate(true);
        reader.init();
        PageForm form = reader.unmarshal(new ByteArrayInputStream(VALID_FORM));
    }

    private PageFormValidatingReader createReader() {
        return new PageFormValidatingReader(new ApiMappings("test"));
    }

    @Test
    public void testPerformance() throws JAXBException, IOException, SAXException {
        long COUNT = 10000;
        {
            PageFormValidatingReader reader = createReader();
            reader.setDoValidate(false);
            reader.init();
            long start = System.nanoTime();
            for (int i = 0; i < COUNT; i++) {
                reader.unmarshal(new ByteArrayInputStream(VALID_FORM));
            }
            System.out.println("Time to unmarshal (not validating) " + (1f * TimeUnit.MILLISECONDS.convert((System.nanoTime() - start), TimeUnit.NANOSECONDS) / COUNT) + " ms/unmarshal");
        }
        {
            PageFormValidatingReader reader = createReader();
            reader.setDoValidate(true);
            reader.init();
            long start = System.nanoTime();
            for (int i = 0; i < COUNT; i++) {
                reader.unmarshal(new ByteArrayInputStream(VALID_FORM));
            }
            System.out.println("Time to unmarshal (validating) " + (1f * TimeUnit.MILLISECONDS.convert((System.nanoTime() - start), TimeUnit.NANOSECONDS) / COUNT) + " ms/unmarshal");
        }

    }
}
