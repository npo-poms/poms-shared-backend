/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import nl.vpro.domain.media.MediaObject;
import nl.vpro.transfer.media.MediaTransfer;
import nl.vpro.transfer.media.PropertySelection;

/**
 * @author Roelof Jan Koekoek
 * @since 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mediaSearchResultType")
@XmlRootElement(name = "mediaSearchResult")
public class MediaTransferSearchResult<T extends MediaObject> extends SearchResult<MediaTransfer<T>> {

/*
    public MediaTransferSearchResult(List<SearchResultItem<MediaTransfer<T>>> list, Long offset, Integer max, Long total) {
        super(list, offset, max, total);
    }

    public static MediaTransferSearchResult create(MediaResult result, PropertySelection selection) {
        List<MediaTransfer> optimised = new ArrayList<>(result.getList().size());
        for(MediaObject mediaObject : result.getList()) {
            optimised.add(MediaTransfer.create(mediaObject, selection));
        }
        return new MediaTransferSearchResult(optimised, result.getOffset(), result.getMax(), result.getTotal());
    }
*/
}
