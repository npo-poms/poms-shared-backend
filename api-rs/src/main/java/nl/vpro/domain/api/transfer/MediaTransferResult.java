/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.transfer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import nl.vpro.domain.api.MediaResult;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.transfer.media.*;

/**
 * @author Roelof Jan Koekoek
 * @since 1.0
 */
@XmlRootElement(name = "mediaResult")
@XmlSeeAlso({ProgramTransfer.class, GroupTransfer.class, SegmentTransfer.class})
public class MediaTransferResult extends Result<MediaTransfer> {

    protected MediaTransferResult() {
    }

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
