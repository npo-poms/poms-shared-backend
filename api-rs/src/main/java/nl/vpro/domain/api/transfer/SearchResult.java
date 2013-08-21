package nl.vpro.domain.api.transfer;


import java.util.AbstractList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.SearchResultItem;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */

@XmlType(name = "searchResultType")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({MediaTransferSearchResult.class, ProgramTransferSearchResult.class})
public class SearchResult<S> extends Result<SearchResultItem<? extends S>> {

    public SearchResult() {
    }

    public SearchResult(List<SearchResultItem<? extends S>> list, Long offset, Integer max, Long total) {
        super(list, offset, max, total);
    }

    public SearchResult(SearchResult<? extends S> copy) {
        super(copy);
    }

    /**
     * Returns a view on this SearchResult as a list of unwrapped objects (so not wrapped by {@link nl.vpro.domain.api.SearchResultItem}s
     */
    public List<S> asList() {
        return new AbstractList<S>() {
            @Override
            public S get(int index) {
                SearchResultItem<? extends S> item = SearchResult.this.getList().get(index);
                return item == null ? null : item.getResult();
            }
            @Override
            public int size() {
                return SearchResult.this.getSize();
            }
        };
    }

    /**
     * Returns this SearchResult as a {@link #Result}, which means that all items are unwrapped, and the facet results are removed.
     */
    public nl.vpro.domain.api.Result<S> asResult() {
        return new Result<>(asList(), getOffset(), getMax(), getTotal());
    }
}
