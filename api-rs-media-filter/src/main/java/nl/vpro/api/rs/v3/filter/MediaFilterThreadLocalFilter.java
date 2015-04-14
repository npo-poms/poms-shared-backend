/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.MDC;

/**
 * @author rico
 */
public class MediaFilterThreadLocalFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest req = (HttpServletRequest) request;
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || "".equals(ip)) {
                ip = req.getRemoteAddr();
            }
            MDC.put("remoteHost", ip);

            ApiMediaFilter.get().clear();
            chain.doFilter(request, response);
        } finally {
            ApiMediaFilter.removeFilter();
            MDC.clear();
        }
    }

    @Override
    public void destroy() {
    }
}
