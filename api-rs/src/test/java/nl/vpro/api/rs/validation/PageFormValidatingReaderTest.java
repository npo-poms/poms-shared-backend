package nl.vpro.api.rs.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import nl.vpro.api.util.ApiMappings;
import nl.vpro.domain.api.page.PageForm;

public class PageFormValidatingReaderTest {

    private static final byte[] VALID_FORM = ("""
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <pagesForm xmlns="urn:vpro:api:2013" xmlns:media="urn:vpro:media:2009" highlight="false">
          <searches>
            <sortDates>
              <matcher>      <begin>2009-01-01T00:00:00+01:00</begin>
              <end>2010-01-01T00:00:00+01:00</end>
              </matcher>    </sortDates>
          </searches>
          <sortFields>
            <sort order="ASC">sortDate</sort>
          </sortFields>
          <facets>
            <sortDates>
              <interval>YEAR</interval>
            </sortDates>
          </facets>
        </pagesForm>
        """).getBytes();

    @Test
    public void testReadFromInvalid() {
        Assertions.assertThatThrownBy(() -> {
            PageFormValidatingReader reader = createReader();
            reader.setDoValidate(true);
            reader.init();

        byte[] bytes = ("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <pagesForm xmlns="urn:vpro:api:2013" xmlns:media="urn:vpro:media:2009" highlight="false">
              <searches>
                <sortDatee>
                  <begin>2009-01-01T00:00:00+01:00</begin>
                  <end>2010-01-01T00:00:00+01:00</end>
                </sortDatee>
              </searches>
              <sortFields>
                <sort order="ASC">sortDate</sort>
              </sortFields>
              <facets>
                <sortDates>
                  <interval>YEAR</interval>
                </sortDates>
              </facets>
            </pagesForm>
            """).getBytes();
        reader.unmarshal(new ByteArrayInputStream(bytes));
        }).isInstanceOf(UnmarshalException.class);
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
