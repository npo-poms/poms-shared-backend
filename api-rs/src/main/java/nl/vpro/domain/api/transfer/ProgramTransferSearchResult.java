/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.transfer;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import nl.vpro.domain.api.ProgramSearchResult;
import nl.vpro.domain.api.SearchResultItem;
import nl.vpro.domain.media.Program;
import nl.vpro.transfer.media.MediaTransfer;
import nl.vpro.transfer.media.ProgramTransfer;
import nl.vpro.transfer.media.PropertySelection;

/**
 * @author Roelof Jan Koekoek
 * @since 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "programSearchResultType")
@XmlRootElement(name = "programSearchResult")
public class ProgramTransferSearchResult extends GenericMediaTransferSearchResult<Program, MediaTransfer<Program>> {

    public ProgramTransferSearchResult(List<SearchResultItem<? extends MediaTransfer<Program>>> list, Long offset, Integer max, Long total) {
        super(list, offset, max, total);
    }

    public static ProgramTransferSearchResult create(ProgramSearchResult result, PropertySelection selection) {
        List<SearchResultItem<? extends MediaTransfer<Program>>> optimised = new ArrayList<>(result.getList().size());
        for(SearchResultItem<? extends Program> searchResultItem : result.getList()) {
            optimised.add(new SearchResultItem<>(
                ProgramTransfer.create(searchResultItem.getResult(), selection),
                searchResultItem.getScore(),
                searchResultItem.getHighlights()
            ));
        }
        return new ProgramTransferSearchResult(optimised, result.getOffset(), result.getMax(), result.getTotal());
    }
}
