package nl.vpro.api.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Date: 20-3-12
 * Time: 11:48
 *
 * @author Ernst Bunders
 * @author http://jpgmr.wordpress.com/2010/07/28/tutorial-implementing-a-servlet-filter-for-jsonp-callback-with-springs-delegatingfilterproxy
 */
public class JsonpCallbackFilter implements Filter {
    private static Logger log = LoggerFactory.getLogger(JsonpCallbackFilter.class);

    public void init(FilterConfig fConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        @SuppressWarnings("unchecked")
        Map<String, String[]> parms = httpRequest.getParameterMap();

        if (parms.containsKey("callback")) {
            if (log.isDebugEnabled())
                log.debug("Wrapping response with JSONP callback '" + parms.get("callback")[0] + "'");

            OutputStream out = httpResponse.getOutputStream();
            GenericResponseWrapper wrapper = new GenericResponseWrapper(httpResponse);
            chain.doFilter(request, wrapper);

            out.write((parms.get("callback")[0] + "(").getBytes());
            out.write(wrapper.getData());
            out.write(");".getBytes());
            wrapper.setContentType("text/javascript;charset=UTF-8");
            out.close();
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }
}
