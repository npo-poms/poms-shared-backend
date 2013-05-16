/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2;

import java.util.List;

import javax.xml.bind.annotation.*;

/**
 *
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@XmlRootElement(name = "pagedResult")
@XmlType(name = "pagedResultType")
public class PagedResult<T> {

    @XmlAttribute
    private Long total;

    @XmlAttribute
    private Long page;

    @XmlElementWrapper(name = "list")
    private List<T> list;

    public List<T> getList() {
        return list;
    }

    public Long getPage() {
        return page;
    }

    @XmlAttribute
    public Long getSize() {
        return Long.valueOf(list.size());
    }

    public Long getTotal() {
        return total;
    }
}
