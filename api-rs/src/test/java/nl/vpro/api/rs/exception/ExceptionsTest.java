/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.exception;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 2.3
 */
class ExceptionsTest {

    @Test
    void notFound() {
        Exception exception = Exceptions.notFound("the message");

        assertThat(exception).isInstanceOf(NotFoundException.class);
        assertThat(exception.getMessage()).isEqualTo("the message");
    }

    @Test
    void notFoundWithArgs() {
        Exception exception = Exceptions.notFound("the message {}", "arg");

        assertThat(exception).isInstanceOf(NotFoundException.class);
        assertThat(exception.getMessage()).isEqualTo("the message arg");
    }

    @Test
    void queryParamNotFound() {
        Exception exception = Exceptions.badRequest("the message");

        assertThat(exception).isInstanceOf(BadRequestException.class);
        assertThat(exception.getMessage()).isEqualTo("the message");
    }

    @Test
    void queryParamNotFoundWithArgs() {
        Exception exception = Exceptions.badRequest("the message {}", "arg");

        assertThat(exception).isInstanceOf(BadRequestException.class);
        assertThat(exception.getMessage()).isEqualTo("the message arg");
    }
}
