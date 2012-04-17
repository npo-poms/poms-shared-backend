/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.service.querybuilder.MediaSearchQuery;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.api.util.UrlProvider;
import nl.vpro.domain.ugc.annotation.Annotation;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.jcouchdb.db.Database;
import org.jcouchdb.db.Options;
import org.jcouchdb.document.ValueRow;
import org.jcouchdb.document.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * User: rico
 * Date: 08/03/2012
 */
@Service("mediaService")
public class MediaServiceImpl implements MediaService {

    public static String MEDIA_CORE_NAME = "poms";
    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    @Autowired
    private UrlProvider urlProvider;

    @Value("${solr.max.result}")
    private int maxResult;

    @Value("${solr.suggest.min.occurrence}")
    private Integer suggestionsMinOccurrence;

    @Value("${solr.suggest.limit}")
    private Integer suggestionsLimit;

    @Autowired
    private SolrServer solrServer;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private Database couchDbMediaServer;

    @Autowired
    private RestTemplate restTemplate;

    public MediaServiceImpl() {
    }

    @Override
    public MediaSearchResult search(String query, String profileName, Integer offset, Integer max) {
        Profile profile = profileService.getProfile(profileName);
        MediaSearchQuery filterQuery = profile.createSearchQuery();
        String filterQueryString = filterQuery.createQueryString();

        SolrQuery solrQuery = new SolrQuery(filterQueryString);
        solrQuery.setFields("*", "score");
        if (StringUtils.isNotBlank(filterQueryString)) {
            solrQuery.setFilterQueries(filterQueryString);
        }
        solrQuery.add("qf", "titleMain^4.0 descriptionMain^2.0");
        solrQuery.setQuery(query);

        Integer queryMaxRows = max != null && max < maxResult ? max : maxResult;
        solrQuery.setRows(queryMaxRows);

        if (offset != null && offset > 0) {
            solrQuery.setStart(offset);
        }

        try {
            QueryResponse response = solrServer.query(solrQuery);
            return conversionService.convert(response, MediaSearchResult.class);
        } catch (SolrServerException e) {
            log.error("Something went wrong submitting the query to solr:", e);
        }
        return new MediaSearchResult();
    }

    @Override
    public MediaSearchSuggestions searchSuggestions(String query, String profileName) {
        Profile profile = profileService.getProfile(profileName);
        MediaSearchQuery filterQuery = profile.createSearchQuery();
        String filterQueryString = filterQuery.createQueryString();

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        if (StringUtils.isNotBlank(filterQueryString)) {
            solrQuery.setFilterQueries(filterQueryString);
        }
        solrQuery.setFacet(true);
        solrQuery.setFacetLimit(suggestionsLimit);
        solrQuery.addFacetField("titleMain", "descriptionMain");
        solrQuery.setFacetPrefix(query);
        solrQuery.setFacetMinCount(suggestionsMinOccurrence);
        solrQuery.setFields("titleMain", "descriptionMain");
        solrQuery.setRows(0);

        try {
            QueryResponse response = solrServer.query(solrQuery);
            return conversionService.convert(response, MediaSearchSuggestions.class);
        } catch (SolrServerException e) {
            log.error("Something went wrong submitting the query to solr:", e);
        }
        return new MediaSearchSuggestions();
    }


    @Override
    public Program getProgram(Long id) {
        String urn = MediaUtil.createUrnFromId(MediaObjectType.program, id);
        ResponseEntity<Program> programResponseEntity = restTemplate.getForEntity("{base}/{urn}", Program.class, urlProvider.getUrl(), urn);
        return programResponseEntity.getBody();
    }

    @Override
    public Annotation getProgramAnnotation(Long id) {
        Program program = getProgram(id);
        return conversionService.convert(program, Annotation.class);
    }

    @Override
    public Group getGroup(Long id, boolean addMembers) {
        String urn = MediaUtil.createUrnFromId(MediaObjectType.group, id);
        ResponseEntity<Group> groupResponseEntity = restTemplate.getForEntity("{base}/{urn}", Group.class, urlProvider.getUrl(), urn);
        Group group = groupResponseEntity.getBody();
        if (addMembers) {
            group.getMembers().addAll(getProgramsForGroup(group));
        }
        return group;
    }

    @Override
    public Segment getSegment(Long id) {
        String urn = MediaUtil.createUrnFromId(MediaObjectType.segment, id);
        ResponseEntity<Segment> segmentResponseEntity = restTemplate.getForEntity("{base}/{urn}", Segment.class, urlProvider.getUrl(), urn);
        return segmentResponseEntity.getBody();
    }


    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

    public void setSuggestionsMinOccurrence(Integer suggestionsMinOccurrence) {
        this.suggestionsMinOccurrence = suggestionsMinOccurrence;
    }

    public void setSuggestionsLimit(Integer suggestionsLimit) {
        this.suggestionsLimit = suggestionsLimit;
    }

    private List<Program> getProgramsForGroup(final Group group) {
        List<Program> programs = new ArrayList<Program>();
        ViewResult<Map> viewResult = null;

        if (group != null) {
            viewResult = getViewResult(group.getUrn(), "media/by-group");
            for (ValueRow<Map> row : viewResult.getRows()) {
                String urn = row.getId();
                if (MediaUtil.getMediaType(urn) == MediaObjectType.program) {
                    Long programId = MediaUtil.getMediaId(MediaObjectType.program, row.getId());
                    Program program = getProgram(programId);
                    if (program != null) {
                        programs.add(program);
                    }
                }

            }
            if (group.isIsOrdered()) {
                Collections.sort(programs, new Comparator<Program>() {
                    @Override
                    public int compare(Program program, Program program1) {
                        return program.getMemberRef(group).getIndex().compareTo(program1.getMemberRef(group).getIndex());
                    }
                });
            } else {
                Collections.sort(programs, new Comparator<Program>() {
                    @Override
                    public int compare(Program program, Program program1) {
                        return -(program.getMemberRef(group).getAdded().compareTo(program1.getMemberRef(group).getAdded()));
                    }
                });
            }
        }
        return programs;
    }

    private ViewResult<Map> getViewResult(final String groupUrn, final String view) {
        return couchDbMediaServer.queryView(view,
            Map.class,
            new Options().startKey(groupUrn)
                .endKey(groupUrn)
                .reduce(false),
            null);
    }

}
