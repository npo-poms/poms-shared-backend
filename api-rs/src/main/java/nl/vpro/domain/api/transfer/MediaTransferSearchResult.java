/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.transfer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

import nl.vpro.domain.api.MediaSearchResult;
import nl.vpro.domain.api.SearchResultItem;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.transfer.media.*;

/**
 * @author Roelof Jan Koekoek
 * @since 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "mediaSearchResultType")
@XmlRootElement(name = "mediaSearchResult")
@XmlSeeAlso({ProgramTransfer.class, GroupTransfer.class, SegmentTransfer.class})
public class MediaTransferSearchResult extends GenericMediaTransferSearchResult<MediaObject, MediaTransfer<MediaObject>> {

    public MediaTransferSearchResult(List<SearchResultItem<? extends MediaTransfer<MediaObject>>> list, Long offset, Integer max, Long total) {
        super(list, offset, max, total);
    }

    public static MediaTransferSearchResult create(MediaSearchResult result, PropertySelection selection) {
        List<SearchResultItem<? extends MediaTransfer<MediaObject>>> optimised = new ArrayList<>(result.getList().size());
        for(SearchResultItem<? extends MediaObject> searchResultItem : result.getList()) {
            optimised.add(new SearchResultItem<>(
                MediaTransfer.create(searchResultItem.getResult(), selection),
                searchResultItem.getScore(),
                searchResultItem.getHighlights()
            ));
        }
        return new MediaTransferSearchResult(optimised, result.getOffset(), result.getMax(), result.getTotal());
    }
}
