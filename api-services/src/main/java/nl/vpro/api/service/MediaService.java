/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import java.util.Iterator;
import java.util.List;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.service.search.fiterbuilder.TagFilter;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchResultItem;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.api.transfer.ProgramList;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.util.rs.error.ServerErrorException;

/**
 * User: rico
 * Date: 08/03/2012
 */
public interface MediaService {

    MediaSearchResult search(String query, TagFilter tagFilter, String profile, Integer offset, Integer maxResult) throws ServerErrorException;

    MediaSearchSuggestions searchSuggestions(String query, TagFilter tagFilter, String profile) throws ServerErrorException;

    String searchES(String index, String[] types, String query) throws ServerErrorException;

    Program getProgram(Long id);

    /**
     * This method returns a number of the most recent programs broadcasted by by the VPRO
     * that has (a) location(s) attached to it, so it is playable.
     * The programs are sorted by first broadcast date descending.
     */
    ProgramList getReplayablePrograms(Integer max, Integer offset, String avType);

    /**
     * @param id
     * @return the POMS segments as one annotation
     */
    Annotation getProgramAnnotation(Long id);

    /**
     * Returns a group, optionally with members.
     * If addMembers=true, and memberTypes is null or empty, all members are returned.
     * If addMembers=true, and specific memberTypes are indicated, only members of those indicated MediaObjectTypes are returned.
     */
    Group getGroup(Long id, boolean addMembers, List<MediaObjectType> memberTypesFilter);

    /**
     * Returns a group, optionally with members.
     * If addMembers=true, and memberTypes is null or empty, all members are returned.
     * If addEpisodes=true, and memberTypes is null or empty, all members are returned.
     * If addMembers=true, and specific memberTypes are indicated, only members of those indicated MediaObjectTypes are returned.
     */
    Group getGroup(Long id, boolean addMembers, boolean addEpisodes, List<MediaObjectType> memberTypesFilter);

    Segment getSegment(Long id);

    Iterator<MediaSearchResultItem> getProfile(String profileName);

}
