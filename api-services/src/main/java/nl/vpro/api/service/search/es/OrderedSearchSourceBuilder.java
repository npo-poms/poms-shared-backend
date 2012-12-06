/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service.search.es;

import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;

import java.util.List;

/**
 * User: rico
 * Date: 06/12/2012
 */
public class OrderedSearchSourceBuilder extends SearchSourceBuilder {

    public OrderedSearchSourceBuilder(List<String> sortFields) {
        super();
        if (sortFields != null && sortFields.size() > 0) {
            for (String sortField : sortFields) {
                SortBuilder sort = ESUtil.parseOrder(sortField);
                if (sort!=null) {
                    sort(sort);
                }
              }
        } else {
            sort(SortBuilders.scoreSort());
        }
    }
}
