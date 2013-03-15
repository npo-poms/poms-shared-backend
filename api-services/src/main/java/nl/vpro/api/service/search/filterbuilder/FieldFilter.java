/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service.search.filterbuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * User: rico
 * Date: 25/02/2013
 */
public class FieldFilter extends SearchFilter<FieldFilter> {
    private static final Logger LOG = LoggerFactory.getLogger(FieldFilter.class);

    private String field;
    private String value;

    public FieldFilter() {
        super(BooleanOp.AND);
    }

    public FieldFilter setField(String field, String value) {
        this.field = field;
        this.value = value;
        return this;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String createQueryString() {
        return field+":"+value;
    }

    @Override
    protected FieldFilter getInstance() {
        return this;
    }

    @Override
    public boolean apply(@Nullable Object o) {
        return o instanceof String;
    }
}
