/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.suggest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.vpro.domain.api.Form;
import nl.vpro.domain.api.SearchResult;
import nl.vpro.domain.api.profile.ProfileDefinition;

/**
 * @author Roelof Jan Koekoek
 * @since 3.2
 */
@Aspect
public class QueryIndexAspect {
    private static final Logger LOG = LoggerFactory.getLogger(QueryIndexAspect.class);

    private final ESQueryRepository queryRepository;

    public QueryIndexAspect(ESQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public Object query(ProceedingJoinPoint pjp, ProfileDefinition<?> profile, Form form, Long offset, Integer max) throws Throwable {
        SearchResult<?> answer = (SearchResult)pjp.proceed(new Object[]{profile, form, offset, max});

        if(answer.getSize() > 0
            && (offset == null || offset == 0)
            && form != null
            && form.getText() != null

            ) {

            try {
                queryRepository.index(new Query(
                        form.getText(),
                        profile != null ? profile.getName() : null)
                );
            } catch(Exception e) {
                LOG.warn("Exception while saving a search query, see root cause", e);
            }
        }

        return answer;
    }
}
