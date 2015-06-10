/**
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.monitoring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.util.ThreadPools;

/**
 * @author Roelof Jan Koekoek
 * @since 3.7
 */
public class NewRelicRunner {
    private static final Logger LOG = LoggerFactory.getLogger(NewRelicRunner.class);

    private List<NewRelicReporter> reporters;

    private static final ScheduledExecutorService EXECUTOR =
        Executors.newScheduledThreadPool(1, ThreadPools.createThreadFactory(NewRelicRunner.class.getSimpleName(), true, Thread.MIN_PRIORITY));

    @PostConstruct
    private void startReporter() {
        LOG.info("Starting the NewRelic custom reporters");

        EXECUTOR.scheduleAtFixedRate(new Runner(), 0, 60, TimeUnit.SECONDS);
    }

    public void setReporters(List<NewRelicReporter> reporters) {
        this.reporters = reporters;
    }

    private class Runner implements Runnable {

        @Override
        public void run() {
            for(NewRelicReporter reporter : reporters) {
                try {
                    LOG.debug("Reporting metrics for {}", reporter.getName());
                    reporter.pollCycle();
                } catch(Exception e) {
                    LOG.error("Error while reporting metrics for {}, root cause {}", reporter.getName(), e.getMessage());
                }
            }
        }
    }
}
