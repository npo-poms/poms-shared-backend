/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.exception;

import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

/**
 * @author rico
 * @since 3.1
 */
@Provider
public class ValidationExceptionProvider implements ExceptionMapper<ResteasyViolationException> {
    @Override
    public Response toResponse(final ResteasyViolationException e) {

        return Response.ok(new nl.vpro.domain.api.Error(Response.Status.BAD_REQUEST.getStatusCode(), "There were one or more constraint violations") {
            @XmlElement
            public List<ResteasyConstraintViolation> getViolations() {
                return e.getViolations();
            }
        }).status(Response.Status.BAD_REQUEST).build();
    }

}
