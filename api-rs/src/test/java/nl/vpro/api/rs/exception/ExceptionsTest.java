/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 2.3
 */
public class ExceptionsTest {

    @Test
    public void testNotFound() {
        Exception exception = Exceptions.notFound("the message");

        assertThat(exception).isInstanceOf(NotFoundException.class);
        assertThat(exception.getMessage()).isEqualTo("the message");
    }

    @Test
    public void testNotFoundWithArgs() {
        Exception exception = Exceptions.notFound("the message {}", "arg");

        assertThat(exception).isInstanceOf(NotFoundException.class);
        assertThat(exception.getMessage()).isEqualTo("the message arg");
    }

    @Test
    public void testQueryParamNotFound() {
        Exception exception = Exceptions.badRequest("the message");

        assertThat(exception).isInstanceOf(BadRequestException.class);
        assertThat(exception.getMessage()).isEqualTo("the message");
    }

    @Test
    public void testQueryParamNotFoundWithArgs() {
        Exception exception = Exceptions.badRequest("the message {}", "arg");

        assertThat(exception).isInstanceOf(BadRequestException.class);
        assertThat(exception.getMessage()).isEqualTo("the message arg");
    }
}
