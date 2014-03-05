/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api.transfer;

import com.google.common.collect.Lists;

import nl.vpro.domain.api.*;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
import nl.vpro.transfer.media.MediaObjectPropertySelectionFunction;
import nl.vpro.transfer.media.ProgramPropertySelectionFunction;
import nl.vpro.transfer.media.PropertySelection;

import java.util.ArrayList;
import java.util.List;

/**
 * User: rico
 * Date: 03/02/2014
 */
public class ResultFilter {
    public static MediaResult filter(MediaResult result, String properties) {
        return filter(result, new PropertySelection(properties));
    }

    public static MediaResult filter(MediaResult result, PropertySelection selection) {
        MediaObjectPropertySelectionFunction function = new MediaObjectPropertySelectionFunction(selection);
        List<? extends MediaObject> optimised = Lists.transform(result.getItems(), function);
        return new MediaResult(optimised, result.getOffset(), result.getMax(), result.getTotal());
    }

    public static MediaSearchResult filter(MediaSearchResult result, String properties) {
        return filter(result, new PropertySelection(properties));
    }

    public static MediaSearchResult filter(MediaSearchResult result, PropertySelection selection) {
        final MediaObjectPropertySelectionFunction function = new MediaObjectPropertySelectionFunction(selection);

        List<SearchResultItem<? extends MediaObject>> optimised = new ArrayList<>(result.getItems().size());
        for(SearchResultItem<? extends MediaObject> searchResultItem : result.getItems()) {
            optimised.add(new SearchResultItem<>(
                function.apply(searchResultItem.getResult()),
                searchResultItem.getScore(),
                searchResultItem.getHighlights()
            ));
        }
        return new MediaSearchResult(optimised, result.getOffset(), result.getMax(), result.getTotal());
    }

    public static ProgramResult filter(ProgramResult result, String properties) {
        return filter(result, new PropertySelection(properties));
    }

    public static ProgramResult filter(ProgramResult result, PropertySelection selection) {
        ProgramPropertySelectionFunction function = new ProgramPropertySelectionFunction(selection);
        List<Program> optimised = Lists.transform(result.getItems(), function);
        return new ProgramResult(optimised, result.getOffset(), result.getMax(), result.getTotal());
    }

    public static ProgramSearchResult filter(ProgramSearchResult result, String properties) {
        return filter(result, new PropertySelection(properties));
    }

    public static ProgramSearchResult filter(ProgramSearchResult result, PropertySelection selection) {
        ProgramPropertySelectionFunction function = new ProgramPropertySelectionFunction(selection);
        List<SearchResultItem<? extends Program>> optimised = new ArrayList<>(result.getItems().size());
        for(SearchResultItem<? extends Program> searchResultItem : result.getItems()) {
            optimised.add(new SearchResultItem<>(
                function.apply(searchResultItem.getResult()),
                searchResultItem.getScore(),
                searchResultItem.getHighlights()
            ));
        }
        return new ProgramSearchResult(optimised, result.getOffset(), result.getMax(), result.getTotal());
    }
}
