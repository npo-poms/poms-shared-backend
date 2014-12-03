/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.media;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author rico
 * @since 3.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/nl/vpro/domain/api/media/mediaServiceSecurityTest-context.xml")
public class MediaServiceSecurityTest {

    @Autowired
    MediaService mediaService;

    @Before
    public void init() {
        SecurityContextHolder.clearContext();
    }

    @Test(expected=AuthenticationCredentialsNotFoundException.class)
    public void testWithoutRole() {
        mediaService.getType("album");
    }

    @Test(expected = AccessDeniedException.class)
    public void testWithWrongRole() {
        Collection roles = Arrays.asList(new SimpleGrantedAuthority("ROLE_API_DOESNOTEXISTS"));
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new TestingAuthenticationToken("user","dontcare", (List<GrantedAuthority>) roles));

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
