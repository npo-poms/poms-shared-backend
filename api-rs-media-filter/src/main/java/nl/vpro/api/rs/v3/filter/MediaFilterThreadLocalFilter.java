/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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
            String query = req.getQueryString();
            String path = req.getRequestURI().substring(req.getContextPath().length());
            MDC.put("request", req.getMethod() + " " + path + (StringUtils.isEmpty(query) ? "" : ("?" + query)));
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
