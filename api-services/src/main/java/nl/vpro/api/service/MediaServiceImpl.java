/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.MediaObject;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.service.searchfilterbuilder.TagFilter;
import nl.vpro.api.service.searchqueryfactory.SolrQueryFactory;
import nl.vpro.api.transfer.*;
import nl.vpro.api.util.UrlProvider;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.jackson.MediaMapper;
import nl.vpro.util.rs.error.NotFoundException;
import nl.vpro.util.rs.error.ServerErrorException;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
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
import java.net.URLDecoder;
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
    private UrlProvider couchdbUrlprovider;

    @Value("${solr.max.result}")
    private int maxResult;

    @Value("${couchdb.view.replayable.programs.by.first.broadcating}")
    private String couchdbViewReplayableRrogramsByFirstBroadcating;

    @Value("${solr.suggest.min.occurrence}")
    private Integer suggestionsMinOccurrence;

    @Value("${solr.suggest.limit}")
    private Integer suggestionsLimit;

    @Autowired
    private ConversionService conversionService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private Database couchDbMediaServer;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SolrQueryFactory solrQueryFactory;

    public MediaServiceImpl() {
    }

    @Override
    public MediaSearchResult search(String query, TagFilter tagFilter, String profileName, Integer offset, Integer max) {
        SolrServer solrServer = solrQueryFactory.getSolrServer();
        Profile profile = profileService.getProfile(profileName, solrServer);
        Integer queryMaxRows = max != null && max < maxResult ? max : maxResult;
        SolrQuery solrQuery = solrQueryFactory.createSearchQuery(profile, query, tagFilter, queryMaxRows, offset);
        if (log.isDebugEnabled()) {
            log.debug("Server: " + ((HttpSolrServer) solrServer).getBaseURL().toString());
            log.debug("Query: " + solrQuery.toString());
        }
        try {
            QueryResponse response = solrServer.query(solrQuery);
            return conversionService.convert(response, MediaSearchResult.class);
        } catch (SolrServerException e) {
            throw new ServerErrorException("Something went wrong submitting search query to solr:" + e.getMessage(), e);
        }
    }


    @Override
    public MediaSearchSuggestions searchSuggestions(String query, TagFilter tagFilter, String profileName) {
        SolrServer solrServer = solrQueryFactory.getSolrServer();
        Profile profile = profileService.getProfile(profileName, solrServer);
        SolrQuery solrQuery = solrQueryFactory.createSuggestQuery(profile, query, tagFilter, suggestionsMinOccurrence, suggestionsLimit);
        if (log.isDebugEnabled()) {
            log.debug("Server: " + ((HttpSolrServer) solrServer).getBaseURL().toString());
            log.debug("Query: " + solrQuery.toString());
        }
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
            ResponseEntity<Program> programResponseEntity = restTemplate.getForEntity("{base}/{urn}", Program.class, couchdbUrlprovider.getUrl(), urn);
            return programResponseEntity.getBody();
        } catch (HttpServerErrorException e) {
            throw new ServerErrorException(e.getMessage(), e);
        } catch (ResourceAccessException e1) {
            throw new ServerErrorException(e1.getMessage(), e1);
        } catch (HttpClientErrorException e3) {
            if (e3.getStatusCode().value() == 404) {
                throw new NotFoundException("Program with id " + id + " could not be found", e3);
            } else {
                throw new ServerErrorException("Something went wrong fetching media with id " + id + ". reason: " + e3.getMessage(), e3);
            }
        }
    }

    @Override
    public ProgramList getReplayablePrograms(Integer max, Integer offset) {
        Options options = new Options().reduce(false);
        if (offset != null) {
            options.skip(offset);
        }
        options.limit(max != null ? max : maxResult);
        options.descending(true);
        options.includeDocs(true);

        // we use a 'now' value with hour precision, so we can have some caching of this query.
        //maybe an hour is too long?
        Calendar now = createNowWithHourPrecision();
        options.startKey(new Object[]{"VPRO", now.getTimeInMillis()});
        options.endKey((new Object[]{"VPRO"}));

        String requestUrl = createCouchdbViewUrl(couchdbViewReplayableRrogramsByFirstBroadcating, options);

        try {
            ResponseEntity<ViewResultWithPrograms> programViewResult = restTemplate.getForEntity(requestUrl, ViewResultWithPrograms.class);

            ProgramList list = new ProgramList();
            list.setNumFound(programViewResult.getBody().getTotalRows());
            list.setStart(programViewResult.getBody().getOffset());
            for (ResultRowWithDocument<Program, String> row : programViewResult.getBody().getRows()) {
                list.addProgram(row.getDoc());
            }

            return list;
        } catch (HttpServerErrorException e) {
            throw new ServerErrorException(e.getMessage(), e);
        } catch (ResourceAccessException e1) {
            throw new ServerErrorException(e1.getMessage(), e1);
        } catch (HttpClientErrorException e3) {
            if (e3.getStatusCode().value() == 404) {
                throw new NotFoundException("View with name " + couchdbViewReplayableRrogramsByFirstBroadcating + " could not be queried", e3);
            } else {
                throw new ServerErrorException("Something went wrong fetching data from couchdb view " + couchdbViewReplayableRrogramsByFirstBroadcating + ". reason: " + e3.getMessage(), e3);
            }
        }
    }

    /**
     * Create an url for querying a couchdb view. We don't use the jcouchdb api because this uses the Svensson json library,
     * which won't work with the jaxb and jackson mapping annotations we already use. Best to use the rest template, that uses
     * Jackson and existing marshalling and unmarshalling 'just works!'
     */
    private String createCouchdbViewUrl(String view, Options options) {
        if (!view.contains("/")) {
            throw new RuntimeException("view must contain a slash to separate the document name from the view name");
        }
        String query = "/_design/" + StringUtils.substringBefore(view, "/") + "/_view/" + StringUtils.substringAfter(view, "/");
        return couchdbUrlprovider.getUrl() + query + URLDecoder.decode(options.toQuery());
    }


    private Calendar createNowWithHourPrecision() {
        Calendar c = new GregorianCalendar(new Locale("NL", "nl"));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        return c;
    }

    @Override
    public Annotation getProgramAnnotation(Long id) {
        Program program = getProgram(id);
        return conversionService.convert(program, Annotation.class);

    }

    @Override
    /**
     * Returns a group, optionally with members.
     * If addMembers=true, and memberTypes is null or empty, all members are returned.
     * If addMembers=true, and specific memberTypes are indicated, only members of those indicated MediaObjectTypes are returned.
     */
    public Group getGroup(Long id, boolean addMembers, List<MediaObjectType> memberTypesFilter) {
        String urn = MediaUtil.createUrnFromId(MediaObjectType.group, id);
        try {
            ResponseEntity<Group> groupResponseEntity = restTemplate.getForEntity("{base}/{urn}", Group.class, couchdbUrlprovider.getUrl(), urn);
            Group group = groupResponseEntity.getBody();
            if (addMembers) {

                group.getMembers().addAll(getMediaForGroup(group, memberTypesFilter)); //MediaObjectType.group, MediaObjectType.program, MediaObjectType.segment
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
        try {
            ResponseEntity<Segment> segmentResponseEntity = restTemplate.getForEntity("{base}/{urn}", Segment.class, couchdbUrlprovider.getUrl(), urn);
            return segmentResponseEntity.getBody();
        } catch (HttpServerErrorException e) {
            throw new ServerErrorException(e.getMessage(), e);
        } catch (ResourceAccessException e1) {
            throw new ServerErrorException(e1.getMessage(), e1);
        } catch (HttpClientErrorException e3) {
            if (e3.getStatusCode().value() == 404) {
                throw new NotFoundException("Segment with id " + id + " could not be found", e3);
            } else {
                throw new ServerErrorException("Something went wrong fetching segment with id " + id + ". reason: " + e3.getMessage(), e3);
            }
        }
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

    /**
     * get the members of a group.
     * Optionally filter on specific types of members.
     * If the filter is null or empty, any member is added to the returned result.
     *
     * @param group             The group of which we want to get the members
     * @param memberTypesFilter List of types to filter the members on, or empty if you want all members regardless its type.
     * @return
     */
    private List<MediaObject> getMediaForGroup(final Group group, List<MediaObjectType> memberTypesFilter) {
        List<MediaObject> mediaObjects = new ArrayList<MediaObject>();
        ViewResult<Map> viewResult;
        List<String> mediaIds = new ArrayList<String>();

        if (group != null) {
            ObjectMapper mapper = new MediaMapper();
            Options options = new Options();
            options.reduce(false);

            viewResult = getViewResult(group.getUrn(), "media/by-group");
            for (ValueRow<Map> row : viewResult.getRows()) {
                String urn = row.getId();

                if (memberTypesFilter == null || memberTypesFilter.isEmpty() || memberTypesFilter.contains(MediaUtil.getMediaType(urn))) {
                    mediaIds.add(urn);
                }

//                MediaObjectType mediaType = MediaUtil.getMediaType(urn);
//                for (MediaObjectType type : typesFilter) {
//                    if (type == mediaType) {
//                        mediaIds.add(urn);
//                    }
//                }
            }
            // Note: the direct view variant does not work with couchdb 1.0.2 time to phase out this library
            ViewAndDocumentsResult<Map, Map> progs = couchDbMediaServer.queryDocumentsByKeys(Map.class, Map.class, mediaIds, new Options(), new JSONParser());
            for (ValueAndDocumentRow<Map, Map> row : progs.getRows()) {
                Map m = row.getDocument();
                String urn = (String) m.get("_id");
                MediaObjectType mediaType = MediaUtil.getMediaType(urn);
                MediaObject mediaObject;
                switch (mediaType) {
                    case group:
                        mediaObject = mapper.convertValue(m, Group.class);
                        mediaObjects.add(mediaObject);
                        break;
                    case program:
                        mediaObject = mapper.convertValue(m, Program.class);
                        mediaObjects.add(mediaObject);
                        break;
                    case segment:
                        mediaObject = mapper.convertValue(m, Segment.class);
                        mediaObjects.add(mediaObject);
                        break;
                    default:
                        log.error("Unknown mediatype for urn : " + urn + " : " + mediaType);
                        break;
                }
            }

            Collections.sort(mediaObjects, group.isIsOrdered() ? new SortInGroupByOrderComparator(group) : new SortInGroupByOrderComparator(group));
        }
        return mediaObjects;
    }

    private ViewResult<Map> getViewResult(final String groupUrn, final String view) {
        return couchDbMediaServer.queryView(view,
            Map.class,
            new Options().startKey(groupUrn)
                .endKey(groupUrn)
                .reduce(false),
            null);
    }


    private static final class SortInGroupByOrderComparator implements Comparator<MediaObject>, Serializable {
        private static final long serialVersionUID = 23450383305L;

        protected final Group group;

        public SortInGroupByOrderComparator(Group group) {
            this.group = group;
        }

        @Override
        public int compare(MediaObject media, MediaObject media1) {
            return media.getMemberRef(group).getIndex().compareTo(media1.getMemberRef(group).getIndex());
        }
    }

    private static final class SortInGroupByDateComparator implements Comparator<MediaObject>, Serializable {
        private static final long serialVersionUID = 23450389305L;
        protected final Group group;

        public SortInGroupByDateComparator(Group group) {
            this.group = group;
        }

        @Override
        public int compare(MediaObject media, MediaObject media1) {
            return -(media.getMemberRef(group).getAdded().compareTo(media1.getMemberRef(group).getAdded()));
        }
    }
}
