/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.service.search.fiterbuilder.TagFilter;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.api.transfer.ProgramList;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.util.rs.error.ServerErrorException;
import org.elasticsearch.action.search.SearchResponse;

import java.util.List;

/**
 * User: rico
 * Date: 08/03/2012
 */
public interface MediaService {

    public MediaSearchResult search(String query, TagFilter tagFilter, String profile, Integer offset, Integer maxResult) throws ServerErrorException;

    public MediaSearchSuggestions searchSuggestions(String query, TagFilter tagFilter, String profile) throws ServerErrorException;

    public String searchES(String index, String[] types, String query) throws ServerErrorException;

    public Program getProgram(Long id);

    /**
     * This method returns a number of the most recent programs broadcasted by by the VPRO
     * that has (a) location(s) attached to it, so it is playable.
     * The programs are sorted by first broadcast date descending.
     */
    public ProgramList getReplayablePrograms(Integer max, Integer offset, String avType);

    /**
     * @param id
     * @return the POMS segments as one annotation
     */
    public Annotation getProgramAnnotation(Long id);

    /**
     * Returns a group, optionally with members.
     * If addMembers=true, and memberTypes is null or empty, all members are returned.
     * If addMembers=true, and specific memberTypes are indicated, only members of those indicated MediaObjectTypes are returned.
     */
    public Group getGroup(Long id, boolean addMembers, List<MediaObjectType> memberTypesFilter);

    public Segment getSegment(Long id);
}
