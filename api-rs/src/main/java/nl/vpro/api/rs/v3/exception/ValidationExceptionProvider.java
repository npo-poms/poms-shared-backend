/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;


import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.annotation.XmlElement;

import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;


/**
 * @author rico
 * @since 3.1
 */
@Provider
public class ValidationExceptionProvider implements ExceptionMapper<ResteasyViolationException> {
    @Override
    public Response toResponse(final ResteasyViolationException e) {
        final String violationsAsString = e.getViolations().toString().replace('\r', ' ');
        return Response
                .ok(new nl.vpro.domain.api.Error(Response.Status.BAD_REQUEST.getStatusCode(), violationsAsString) {
            @XmlElement
            public List<ResteasyConstraintViolation> getViolations() {
                return e.getViolations();
            }

            @Override
            public String toString() {
                return (super.toString() + ":" + violationsAsString);
            }
        })
                .status(Response.Status.BAD_REQUEST)
                .build();
    }

}
