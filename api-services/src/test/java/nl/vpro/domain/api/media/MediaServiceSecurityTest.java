/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author rico
 * @since 3.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/nl/vpro/domain/api/media/mediaServiceSecurityTest-context.xml")
public class MediaServiceSecurityTest {

    @Autowired
    MediaService mediaService;

    @BeforeEach
    public void init() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testWithoutRole() {
        assertThatThrownBy(() ->
            mediaService.getType("album")
        ).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    public void testWithWrongRole() {
        assertThatThrownBy(() -> {

            Collection roles = Arrays.asList(new SimpleGrantedAuthority("ROLE_API_DOESNOTEXISTS"));
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(new TestingAuthenticationToken("user","dontcare", (List<GrantedAuthority>) roles));
        }).isInstanceOf(AccessDeniedException.class);

        mediaService.getType("album");
    }

    @Test
    public void testWithRightRole() {
        Collection roles = Arrays.asList(new SimpleGrantedAuthority("ROLE_API_CLIENT"));
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new TestingAuthenticationToken("user","dontcare", (List<GrantedAuthority>) roles));

        mediaService.getType("album");
    }
}
