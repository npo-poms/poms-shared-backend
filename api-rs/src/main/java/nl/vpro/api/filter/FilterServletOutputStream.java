package nl.vpro.api.filter;

/**
 * Date: 20-3-12
 * Time: 11:55
 *
 * @author Ernst Bunders
 * @author http://jpgmr.wordpress.com/2010/07/28/tutorial-implementing-a-servlet-filter-for-jsonp-callback-with-springs-delegatingfilterproxy
 */

import javax.servlet.ServletOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FilterServletOutputStream extends ServletOutputStream {

    private DataOutputStream stream;

    public FilterServletOutputStream(OutputStream output) {
        stream = new DataOutputStream(output);
    }

    public void write(int b) throws IOException {
        stream.write(b);
    }

    public void write(byte[] b) throws IOException {
        stream.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        stream.write(b, off, len);
    }
}