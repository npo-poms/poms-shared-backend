/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import nl.vpro.domain.media.MediaObject;
import nl.vpro.transfer.media.MediaTransfer;
import nl.vpro.transfer.media.PropertySelection;

/**
 * @author Roelof Jan Koekoek
 * @since 1.0
 */
@XmlRootElement(name = "mediaResult")
@XmlType(name = "mediaResultType")
public class MediaTransferResult extends Result<MediaTransfer> {

    public MediaTransferResult(List<? extends MediaTransfer> list, Long offset, Integer max, Long total) {
        super(list, offset, max, total);
    }

    public static MediaTransferResult create(MediaResult result, PropertySelection selection) {
        List<MediaTransfer> optimised = new ArrayList<>(result.getList().size());
        for(MediaObject mediaObject : result.getList()) {
            optimised.add(MediaTransfer.create(mediaObject, selection));
        }
        return new MediaTransferResult(optimised, result.getOffset(), result.getMax(), result.getTotal());
    }
}
