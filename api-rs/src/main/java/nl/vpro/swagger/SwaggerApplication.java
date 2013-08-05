/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.swagger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Application;

import org.springframework.beans.factory.annotation.Autowired;

import com.wordnik.swagger.jaxrs.JaxrsApiReader;

import nl.vpro.api.rs.v2.media.MediaRestService;
import nl.vpro.api.rs.v2.page.PageRestService;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
public class SwaggerApplication extends Application {
    private static final Set<Object> singletons = new HashSet<>();

    @PostConstruct
    private void init() {
        JaxrsApiReader.setFormatString("");
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<Class<?>>();
        for(Object singleton : singletons) {
            set.add(singleton.getClass());
        }
        return set;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    @Autowired
    private void inject(PageRestService pageRestService) {
        singletons.add(pageRestService);
    }

    @Autowired
    private void inject(MediaRestService mediaRestService) {
        singletons.add(mediaRestService);
    }

    private static void inject(Object... services) {
        singletons.addAll(Arrays.asList(services));
    }
}
