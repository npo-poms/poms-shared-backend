/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.service.searchfilterbuilder.TagFilter;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.domain.ugc.annotation.Annotation;

/**
 * User: rico
 * Date: 08/03/2012
 */
public interface MediaService {

    public MediaSearchResult search(String query, TagFilter tagFilter, String profile, Integer offset, Integer max);

    public MediaSearchSuggestions searchSuggestions(String query, TagFilter tagFilter, String profile);

    public Program getProgram(Long id);

    public Annotation getProgramAnnotation(Long id);

    public Group getGroup(Long id, boolean addMembers);

    public Segment getSegment(Long id);
}
