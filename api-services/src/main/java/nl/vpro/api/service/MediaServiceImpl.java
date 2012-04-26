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
import nl.vpro.api.rs.error.NotFoundException;
import nl.vpro.api.rs.error.ServerErrorException;
import nl.vpro.api.service.querybuilder.MediaSearchQuery;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.api.util.UrlProvider;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.jackson.MediaMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.jcouchdb.db.Database;
import org.jcouchdb.db.Options;
import org.jcouchdb.document.ValueAndDocumentRow;
import org.jcouchdb.document.ValueRow;
import org.jcouchdb.document.ViewAndDocumentsResult;
import org.jcouchdb.document.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.svenson.JSONParser;

import java.io.Serializable;
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
            throw new ServerErrorException("Something went wrong submitting search query to solr:" + e.getMessage(), e);
        }
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
            throw new ServerErrorException("Something went wrong submitting search query to solr:" + e.getMessage(), e);
        }
    }


    @Override
    public Program getProgram(Long id) {
        String urn = MediaUtil.createUrnFromId(MediaObjectType.program, id);
        try {
            ResponseEntity<Program> programResponseEntity = restTemplate.getForEntity("{base}/{urn}", Program.class, urlProvider.getUrl(), urn);
            return programResponseEntity.getBody();
        } catch (HttpServerErrorException e) {
            throw new ServerErrorException(e.getMessage(), e);
        } catch (ResourceAccessException e1) {
            throw new ServerErrorException(e1.getMessage(), e1);
        } catch (HttpClientErrorException e3) {
            if (e3.getStatusCode().value() == 404) {
                throw new NotFoundException("Program with id " + id + " could not be found", e3);
            } else {
                throw new ServerErrorException("Something went wrong fetching program with id " + id + ". reason: " + e3.getMessage(), e3);
            }
        }
    }

    @Override
    public Annotation getProgramAnnotation(Long id) {
        Program program = getProgram(id);
        return conversionService.convert(program, Annotation.class);

    }

    @Override
    public Group getGroup(Long id, boolean addMembers) {
        String urn = MediaUtil.createUrnFromId(MediaObjectType.group, id);
        try {
            ResponseEntity<Group> groupResponseEntity = restTemplate.getForEntity("{base}/{urn}", Group.class, urlProvider.getUrl(), urn);
            Group group = groupResponseEntity.getBody();
            if (addMembers) {
                group.getMembers().addAll(getProgramsForGroup(group));
            }
            return group;
        } catch (HttpServerErrorException e) {
            throw new ServerErrorException(e.getMessage(), e);
        } catch (ResourceAccessException e1) {
            throw new ServerErrorException(e1.getMessage(), e1);
        } catch (HttpClientErrorException e3) {
            if (e3.getStatusCode().value() == 404) {
                throw new NotFoundException("Group with id " + id + " could not be found", e3);
            } else {
                throw new ServerErrorException("Something went wrong fetching group with id " + id + ". reason: " + e3.getMessage(), e3);
            }
        }
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
        ViewResult<Map> viewResult;
        List<String> programIds = new ArrayList<String>();

        if (group != null) {
            ObjectMapper mapper = new MediaMapper();
            Options options = new Options();
            options.reduce(false);

            viewResult = getViewResult(group.getUrn(), "media/by-group");
            for (ValueRow<Map> row : viewResult.getRows()) {
                String urn = row.getId();
                if (MediaUtil.getMediaType(urn) == MediaObjectType.program) {
                    programIds.add(urn);
                }
            }
            // Note: the direct view variant does not work with couchdb 1.0.2 time to phase out this library
            ViewAndDocumentsResult<Map, Map> progs = couchDbMediaServer.queryDocumentsByKeys(Map.class, Map.class, programIds, new Options(), new JSONParser());
            for (ValueAndDocumentRow<Map, Map> row : progs.getRows()) {
                Map m = row.getDocument();
                Program program = mapper.convertValue(m, Program.class);
                programs.add(program);
            }

            Collections.sort(programs, group.isIsOrdered() ? new SortInGroupByOrderComparator(group) : new SortInGroupByOrderComparator(group));
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


    private static final class SortInGroupByOrderComparator implements Comparator<Program>, Serializable {
        private static final long serialVersionUID=23450383305L;

        protected final Group group;

        public SortInGroupByOrderComparator(Group group) {
            this.group = group;
        }

        @Override
        public int compare(Program program, Program program1) {
            return program.getMemberRef(group).getIndex().compareTo(program1.getMemberRef(group).getIndex());
        }
    }

    private static final class SortInGroupByDateComparator implements Comparator<Program>, Serializable {
        private static final long serialVersionUID=23450389305L;
        protected final Group group;

        public SortInGroupByDateComparator(Group group) {
            this.group = group;
        }

        @Override
        public int compare(Program program, Program program1) {
            return -(program.getMemberRef(group).getAdded().compareTo(program1.getMemberRef(group).getAdded()));
        }
    }
}
