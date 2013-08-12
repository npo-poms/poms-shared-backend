/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

import nl.vpro.domain.media.MediaObject;
import nl.vpro.transfer.media.MediaTransfer;
import nl.vpro.transfer.media.PropertySelection;

/**
 * @author Roelof Jan Koekoek
 * @since 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "genericMediaSearchResultType", propOrder = {"facets"})
public abstract class GenericMediaTransferSearchResult<M extends MediaObject, T extends MediaTransfer<M>> extends SearchResult<T> {

    @XmlElement
    private MediaFacetsResult facets;

    public GenericMediaTransferSearchResult(List<SearchResultItem<? extends T>> list, Long offset, Integer max, Long total) {
        super(list, offset, max, total);
    }

    public MediaFacetsResult getFacets() {
        return facets;
    }

    public void setFacets(MediaFacetsResult facets) {
        this.facets = facets;
    }
}
