/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;

import nl.vpro.logging.mdc.MDCConstants;

/**
 * @author rico
 */
@Log4j2
public class MediaFilterThreadLocalFilter extends HttpFilter {


    @Override
    public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        try {
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = req.getRemoteAddr();
            }
            String query = req.getQueryString();
            String path = req.getRequestURI().substring(req.getContextPath().length());
            ThreadContext.put(MDCConstants.REQUEST, req.getMethod() + " " + path + (StringUtils.isEmpty(query) ? "" : ("?" + query)));
            ThreadContext.put(MDCConstants.REMOTE_ADDR, ip);

            ApiMediaFilter.removeFilter();

            chain.doFilter(req, res);
        } catch (ServletException | IOException | RuntimeException ioe) {
            if (ioe.getCause() != null && ioe.getCause().getClass().getSimpleName().equals("ClientAbortException")) {
                // NPA-346 Don't log client errors!
                log.info("{} (cause: {} {}). Seems like the client aborted. This can be ignored", ioe.getMessage(), ioe.getCause().getClass().getName(), ioe.getCause().getMessage());
                return;
            }
            throw ioe;
        } finally {
            ApiMediaFilter.removeFilter();
            ThreadContext.clearAll();
        }
    }

}
