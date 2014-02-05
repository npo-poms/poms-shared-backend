/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.exception;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.junit.Test;

import static nl.vpro.fest.assertions.MediaAssertions.assertThat;

/**
 * @author Roelof Jan Koekoek
 * @since 2.3
 */
public class ExceptionsTest {

    @Test
    public void testNotFound() throws Exception {
        Exception exception = Exceptions.notFound("the message");

        assertThat(exception).isInstanceOf(NotFoundException.class);
        assertThat(exception.getMessage()).isEqualTo("the message");
    }

    @Test
    public void testNotFoundWithArgs() throws Exception {
        Exception exception = Exceptions.notFound("the message {}", "arg");

        assertThat(exception).isInstanceOf(NotFoundException.class);
        assertThat(exception.getMessage()).isEqualTo("the message arg");
    }

    @Test
    public void testQueryParamNotFound() throws Exception {
        Exception exception = Exceptions.badRequest("the message");

        assertThat(exception).isInstanceOf(BadRequestException.class);
        assertThat(exception.getMessage()).isEqualTo("the message");
    }

    @Test
    public void testQueryParamNotFoundWithArgs() throws Exception {
        Exception exception = Exceptions.badRequest("the message {}", "arg");

        assertThat(exception).isInstanceOf(BadRequestException.class);
        assertThat(exception.getMessage()).isEqualTo("the message arg");
    }
}
