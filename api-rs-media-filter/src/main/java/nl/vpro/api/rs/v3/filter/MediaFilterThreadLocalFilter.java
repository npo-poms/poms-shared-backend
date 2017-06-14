/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

/**
 * @author rico
 */
@Slf4j
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

            ApiMediaFilter.removeFilter();

            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException ioe) {
            if (ioe.getCause() != null && ioe.getCause().getClass().getSimpleName().equals("ClientAbortException")) {
                // NPA-346 Don't log client errors!
                log.info("{} (cause: {} {}). Seems like the client aborted. This can be ignored", ioe.getMessage(), ioe.getCause().getClass().getName(), ioe.getCause().getMessage());
                return;
            }
            throw ioe;
        } finally {

            ApiMediaFilter.removeFilter();
            MDC.clear();
        }
    }

    @Override
    public void destroy() {
    }
}
