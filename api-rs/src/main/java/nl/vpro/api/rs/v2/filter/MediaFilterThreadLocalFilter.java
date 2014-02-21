/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.filter;

import javax.servlet.*;
import java.io.IOException;

/**
 * User: rico
 * Date: 20/02/2014
 */
public class MediaFilterThreadLocalFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            ApiMediaFilter.get().clear();
            doFilter(request, response, chain);
        } finally {
            ApiMediaFilter.removeFilter();
        }
    }

    @Override
    public void destroy() {
    }
}
