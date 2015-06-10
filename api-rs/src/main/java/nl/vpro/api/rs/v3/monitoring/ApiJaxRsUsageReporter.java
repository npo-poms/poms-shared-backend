/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.monitoring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.*;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.newrelic.api.agent.NewRelic;

import nl.vpro.api.security.ApiAuthenticationToken;

/**
 * @author Roelof Jan Koekoek
 * @since 3.7
 */
@Aspect
public class ApiJaxRsUsageReporter implements NewRelicReporter {

    private ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>(20);

    @Before("execution(* nl.vpro.api.rs.v3.**.*RestService.*(..))")
//    @Before("execution(* nl.vpro.api.rs.v3.media.MediaRestService.*(..)) or execution(* nl.vpro.api.rs.v3.page.PageRestService.*(..)) or execution(* nl.vpro.api.rs.v3.schedule.ScheduleRestService.*(..)) or execution(* nl.vpro.api.rs.v3.profile.ProfileRestService.*(..)) or execution(* nl.vpro.api.rs.v3.tvvod.TVVodRestService.*(..))")
//    @Before("@annotation(javax.ws.rs.GET) or @annotation(javax.ws.rs.POST) or @annotation(javax.ws.rs.DELETE) or @annotation(javax.ws.rs.PUT)")
    public void recordMetrics(JoinPoint joinPoint) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !(authentication instanceof ApiAuthenticationToken)) {
            return;
        }

        countUser(authentication);

        final StringBuilder pathBuilder = getMetricPath(joinPoint);

        countMethod(authentication, pathBuilder.toString());
    }

    @Override
    public String getName() {
        return "ApiUsage";
    }

    @Override
    public void pollCycle() {
        for(Map.Entry<String, WindowCounter> entry : counters.entrySet()) {
            NewRelic.recordMetric(entry.getKey(), entry.getValue().getRatio(TimeUnit.SECONDS));
        }
    }

    private void countUser(Authentication authentication) {
        StringBuilder sb = new StringBuilder("Custom/Account/")
            .append(authentication.getPrincipal())
            .append("[calls/second]");

        String key = sb.toString();

        increment(key);
    }

    private void countMethod(Authentication authentication, String path) {
        StringBuilder sb = new StringBuilder("Custom/")
            .append(authentication.getPrincipal())
            .append('/')
            .append(path);

        String key = sb.toString();

        increment(key);
    }

    private void increment(String key) {
        WindowCounter windowCounter = counters.get(key);
        if(windowCounter == null) {
            counters.putIfAbsent(key, new WindowCounter());
        }
        windowCounter = counters.get(key);
        windowCounter.incrementAndGet();
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
        sb.append('(');
        sb.append(httpMethod);
        sb.append(')');

        sb.append("[calls/second]");

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
