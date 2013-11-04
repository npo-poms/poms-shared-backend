/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.transfer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import nl.vpro.domain.api.ProgramResult;
import nl.vpro.domain.media.Program;
import nl.vpro.transfer.media.MediaTransfer;
import nl.vpro.transfer.media.ProgramTransfer;
import nl.vpro.transfer.media.PropertySelection;

/**
 * @author Roelof Jan Koekoek
 * @since 1.0
 */
@XmlRootElement(name = "programResult")
@XmlType(name = "programResultType")
public class ProgramTransferResult extends Result<MediaTransfer> {

    protected ProgramTransferResult() {
    }

    public ProgramTransferResult(List<? extends ProgramTransfer> list, Long offset, Integer max, Long total) {
        super(list, offset, max, total);
    }

    public static ProgramTransferResult create(ProgramResult result, PropertySelection selection) {
        List<ProgramTransfer> optimised = new ArrayList<>(result.getList().size());
        for(Program mediaObject : result.getList()) {
            optimised.add(ProgramTransfer.create(mediaObject, selection));
        }
        return new ProgramTransferResult(optimised, result.getOffset(), result.getMax(), result.getTotal());
    }
}
