/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.monitoring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.*;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import nl.vpro.api.security.ApiAuthenticationToken;
import nl.vpro.spring.security.ldap.LdapEditor;

/**
 * @author Roelof Jan Koekoek
 * @since 3.7
 */
@Aspect
public class ApiJaxRsUsageReporter implements NewRelicReporter {
    private static final Logger LOG = LoggerFactory.getLogger(ApiJaxRsUsageReporter.class);

    private static final String UNIT = "calls/second";

    private final ConcurrentHashMap<String, WindowedMetric> counters = new ConcurrentHashMap<>(20);

    @Before("execution(* nl.vpro.api.rs.v3.**.*RestService.*(..))")
//    @Before("execution(* nl.vpro.api.rs.v3.media.MediaRestService.*(..)) or execution(* nl.vpro.api.rs.v3.page.PageRestService.*(..)) or execution(* nl.vpro.api.rs.v3.schedule.ScheduleRestService.*(..)) or execution(* nl.vpro.api.rs.v3.profile.ProfileRestService.*(..)) or execution(* nl.vpro.api.rs.v3.tvvod.TVVodRestService.*(..))")
//    @Before("@annotation(javax.ws.rs.GET) or @annotation(javax.ws.rs.POST) or @annotation(javax.ws.rs.DELETE) or @annotation(javax.ws.rs.PUT)")
    public void recordMetrics(JoinPoint joinPoint) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null) {
            return;
        }

        final String principalId = getPrincipalId(authentication);

        final String path = getMetricPath(joinPoint).toString();
        countUser(path, principalId);
        countMethod(path, principalId);
    }

    @Override
    public Collection<WindowedMetric> getMetrics() {
        LOG.debug("Providing metrics {}", counters.values());
        return counters.values();
    }

    private void countUser(String path, String principalId) {
        StringBuilder sb = new StringBuilder()
            .append("Account/")
            .append(principalId)
            .append(path);

        increment(sb.toString());
    }

    private void countMethod(String path, String principalId) {

        StringBuilder sb = new StringBuilder()
            .append("Operation")
            .append(path)
            .append('/')
            .append(principalId);

        increment(sb.toString());
    }

    private String getPrincipalId(Authentication authentication) {
        final Object principal = authentication.getPrincipal();

        String principalId = null;
        if(principal instanceof String) {
            principalId = (String)principal;
        } else if (principal instanceof LdapEditor) {
            principalId = ((LdapEditor)principal).getUsername();
        } else throw new IllegalArgumentException("No support for principal type " + principal.getClass());

        return principalId;
    }

    private void increment(String key) {
        WindowedMetric windowCounter = counters.get(key);
        if(windowCounter == null) {
            LOG.debug("Adding NewRelic counter {}/{}", key, UNIT);
            counters.putIfAbsent(key, new WindowedMetric(key, UNIT, TimeUnit.SECONDS));
        }

        windowCounter = counters.get(key);
        windowCounter.increment();
    }

    private StringBuilder getMetricPath(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();

        StringBuilder sb = new StringBuilder();

        String segment = findPath(method.getDeclaringClass().getAnnotations());
        if(segment != null) {
            if(!segment.startsWith("/")) {
                sb.append('/');
            }
            sb.append(segment);
        }

        segment = findPath(method.getAnnotations());
        if(segment != null) {
            if(sb.charAt(sb.length() - 1) != '/' && !segment.startsWith("/")) {
                sb.append('/');
            }
            sb.append(segment);
        }

        final String httpMethod = findHttpMethod(method.getAnnotations());
        sb.append(" (");
        sb.append(httpMethod);
        sb.append(')');

        return sb;
    }

    private String findPath(Annotation[] annotations) {
        if(annotations == null) {
            return null;
        }

        for(Annotation annotation : annotations) {
            if(annotation instanceof Path) {
                return ((Path)annotation).value();
            }
        }

        return null;
    }

    private String findHttpMethod(Annotation[] annotations) {
        if(annotations == null) {
            return null;
        }

        for(Annotation annotation : annotations) {
            if(annotation instanceof GET) {
                return "GET";
            }
            if(annotation instanceof POST) {
                return "POST";
            }
            if(annotation instanceof PUT) {
                return "PUT";
            }
            if(annotation instanceof DELETE) {
                return "DELETE";
            }
        }

        return null;
    }
}
