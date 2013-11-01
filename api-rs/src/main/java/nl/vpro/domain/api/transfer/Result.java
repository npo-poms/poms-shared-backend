/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.transfer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * @author Roelof Jan Koekoek
 * @since 2.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "resultType", propOrder = {"list"})
@XmlSeeAlso({ProgramTransferResult.class, MediaTransferResult.class})
public class Result<T> implements Iterable<T> {

    @XmlAttribute
    private Long total;

    @XmlAttribute
    private Long offset = 0l;

    @XmlAttribute
    private Integer max;

    @XmlElementWrapper(name = "list")
    @XmlElement(name = "item")
    private List<? extends T> list;

    public Result() {
    }

    public Result(List<? extends T> list, Long offset, Integer max, Long total) {
        this.list = list;
        this.offset = offset;
        this.max = max;
        this.total = total;
    }

    public Result(Result<? extends T> copy) {
        this.list = copy.list;
        this.offset = copy.offset;
        this.max = copy.max;
        this.total = copy.total;
    }

    public List<? extends T> getList() {
        return list;
    }

    /**
     * The offset (base 0) of the list.
     */
    public Long getOffset() {
        return offset;
    }

    /**
     * The maximum size of this list as originally requested by the client.
     */
    public Integer getMax() {
        return max;
    }

    /**
     * The actual size of this list.
     */
    public Integer getSize() {
        return list == null ? 0 : list.size();
    }


    /**
     * The size of the list if no offset or max would have been applied.
     */
    public Long getTotal() {
        return total;
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(getList()).iterator();
    }

    @Override
    public String toString() {
        return "" + getList();

    }
}
