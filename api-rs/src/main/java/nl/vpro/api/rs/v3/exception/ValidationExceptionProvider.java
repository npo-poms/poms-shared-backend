/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;


import lombok.extern.slf4j.Slf4j;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.annotation.XmlElement;

import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;

import nl.vpro.api.rs.v3.interceptors.StoreRequestInThreadLocal;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;


/**
 * @author rico
 * @since 3.1
 */
@Provider
@Slf4j
public class ValidationExceptionProvider implements ExceptionMapper<ResteasyViolationException> {
    @Override
    public Response toResponse(final ResteasyViolationException e) {
        final String violationsAsString = e.getViolations().toString().replace('\r', ' ');

        log.info("Bad request/Violation exception: {}. Request: {}", violationsAsString, StoreRequestInThreadLocal.getRequestBody());

        return Response
                .status(BAD_REQUEST)
                .entity(new nl.vpro.domain.api.Error(BAD_REQUEST, violationsAsString) {
            @XmlElement
            public List<ResteasyConstraintViolation> getViolations() {
                return e.getViolations();
            }

            @Override
            public String toString() {
                return (super.toString() + ":" + violationsAsString);
            }
        })
                .build();
    }

}
