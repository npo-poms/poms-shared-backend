/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service;

import nl.vpro.api.domain.media.*;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.service.search.Search;
import nl.vpro.api.service.search.es.ProfileFilterBuilder;
import nl.vpro.api.service.search.filterbuilder.MediaSearchFilter;
import nl.vpro.api.service.search.filterbuilder.SearchFilter;
import nl.vpro.api.service.search.filterbuilder.TagFilter;
import nl.vpro.api.transfer.*;
import nl.vpro.api.util.CouchdbViewIterator;
import nl.vpro.api.util.MediaObjectIterator;
import nl.vpro.api.util.UrlProvider;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.jackson.MediaMapper;
import nl.vpro.util.FilteringIterator;
import nl.vpro.util.WrappedIterator;
import nl.vpro.util.rs.error.NotFoundException;
import nl.vpro.util.rs.error.ServerErrorException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * User: rico
 * Date: 08/03/2012
 */
@Service("mediaService")
public class MediaServiceImpl implements MediaService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaService.class);

    @Autowired
    private UrlProvider couchdbUrlprovider;

    @Value("${solr.max.result}")
    private int globalMaxResult;

    @Value("${couchdb.view.replayable.programs.by.first.broadcasting}")
    private String couchdbViewReplayableRrogramsByFirstBroadcasting;

    @Value("${couchdb.view.replayable.programs.by.avtype}")
    private String couchdbViewReplayableRrogramsByAvtype;

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
    private Search search;

    @Autowired
    private Client esClient;

    public static MediaService INSTANCE;

    private final ObjectMapper mapper = new MediaMapper();

    public MediaServiceImpl() {
        if (INSTANCE != null) throw new IllegalStateException();
        INSTANCE = this;
    }

    /**
     * Search calls
     */

    @Override
    public MediaSearchResult search(String query, TagFilter tagFilter, String profileName, Integer offset, Integer maxResult) throws ServerErrorException {
        Profile profile = profileService.getProfile(profileName);
        Integer queryMaxRows = maxResult != null && maxResult < globalMaxResult ? maxResult : globalMaxResult;
        return search.search(profile, query, tagFilter, offset, queryMaxRows);
    }


    @Override
    public SearchSuggestions searchSuggestions(String query, TagFilter tagFilter, String profileName) throws ServerErrorException {
        Profile profile = profileService.getProfile(profileName);
        return search.suggest(profile, query, tagFilter, Collections.<String>emptyList(), suggestionsMinOccurrence, suggestionsLimit);
    }

    @Override
    public String searchES(String index, String[] types, String query) throws ServerErrorException {
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest
                .searchType(SearchType.DEFAULT)
                .source(query);
        if (types != null && types.length > 0) {
            searchRequest.types(types);
        }

        ActionFuture<SearchResponse> responseFuture = esClient.search(searchRequest);
        try {
            return responseFuture.get().toString();
        } catch (InterruptedException e) {
            throw new ServerErrorException("Interrupted while executing ES query [" + query + "] on index " + index + ": " + e.getMessage());
        } catch (ExecutionException e) {
            throw new ServerErrorException("something went wrong executing ES query [" + query + "] on index " + index + ": " + e.getMessage(), e);
        }
    }


    /**
     * Program calls
     */

    @Override
    public Program getProgram(Long id) {
        String urn = MediaUtil.createUrnFromId(MediaObjectType.program, id);
        try {
            ResponseEntity<Program> programResponseEntity = restTemplate.getForEntity("{base}/{urn}", Program.class, couchdbUrlprovider.getUrl(), urn);
            return programResponseEntity.getBody();
        } catch (HttpServerErrorException e) {
            throw new ServerErrorException(couchdbUrlprovider.getUrl() + ": " + e.getMessage(), e);
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
    public ProgramList getReplayablePrograms(Integer max, Integer offset, AvType avType) {
        final SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        final Profile vpro = profileService.getProfile("vpro");
        final FilterBuilder filterBuilder = new ProfileFilterBuilder(vpro);
        sourceBuilder.filter(filterBuilder);

        if (max    != null) sourceBuilder.size(max);
        if (offset != null) sourceBuilder.from(offset);

        final BoolQueryBuilder queryBuilder = new BoolQueryBuilder()
            .must(QueryBuilders.prefixQuery("urn", "urn:vpro:media:program")); // Only return programs

        if (avType != null) queryBuilder.must(QueryBuilders.termQuery("avType", avType.name().toLowerCase()));

        sourceBuilder.query(queryBuilder);

        sourceBuilder.sort("sortDate", SortOrder.DESC);

        final SearchRequest request = new SearchRequest("poms").source(sourceBuilder);
        final ActionFuture<SearchResponse> responseFuture = esClient.search(request);

        try {
            final SearchResponse response = responseFuture.actionGet();
            final SearchHits hits = response.hits();
            return toProgramList(hits, offset);
        } catch (Throwable e) {
            throw new ServerErrorException("Something went wrong performing an elastic search operation: " + e.getMessage(), e);
        }
    }

    @Override
    public Iterator<Program> getAllReplayablePrograms(AvType avType) {
        try {
            URL requestUrl = new URL(getReplayableProgramsCouchdbUrl(null, null, avType, true));

            InputStream inputStream = requestUrl.openStream();
            final ObjectMapper m = new ObjectMapper();

            Iterator<JsonNode> iterator = new CouchdbViewIterator(inputStream);
            return new WrappedIterator<JsonNode, Program>(iterator) {
                private final Logger log = LoggerFactory.getLogger(MediaService.class);

                Program next;
                @Override
                public Program next() {
                    findNext();
                    if (next == null) throw new NoSuchElementException();
                    Program result = next;
                    next = null;
                    return result;
                }
                @Override
                public boolean hasNext() {
                    findNext();
                    return next != null;
                }

                protected void findNext() {
                    if (next == null) {
                        while (wrapped.hasNext()) {
                            JsonNode jsonNode = wrapped.next();
                            try {
                                final Program program = m.readValue(jsonNode, Program.class);
                                final List<Image> images = program.getImages();
                                if (images != null && !images.isEmpty()) {
                                    next = program;
                                    break;
                                }
                            } catch (IOException e) {
                                log.error(jsonNode.get("_id") + ": " + e.getMessage());
                            }
                        }
                    }
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ProgramList toProgramList(SearchHits hits, Integer offset) {

        final long numFound = hits.totalHits();
        if (offset == null) {
            offset = 0;
        }
        final ProgramList programList = new ProgramList(numFound, offset);
        for (SearchHit hit : hits.getHits()) {
            try {
                final Program program = mapper.readValue(hit.getSourceAsString(), Program.class);
                programList.addProgram(program);
            } catch (Throwable t) {
                LOG.error(t.getClass().getName() + " " + t.getMessage());
            }
        }
        return programList;
    }

    private String getReplayableProgramsCouchdbUrl(Integer offset, Integer max, AvType avType, boolean includeDocs) {
        Options options = new Options();
        options.reduce(false);
        options.descending(true);
        options.includeDocs(includeDocs);

        if (offset != null) {
            options.skip(offset);
        }
        if (max != null) {
            options.limit(max);
        }

        // we use a 'now' value with hour precision, so we can have some caching of this query.
        //maybe an hour is too long?
        Calendar now = createNowWithHourPrecision();
        final String view;
        if (avType == null) {
            options.startKey(new Object[]{"VPRO", now.getTimeInMillis()});
            options.endKey((new Object[]{"VPRO"}));
            view = couchdbViewReplayableRrogramsByFirstBroadcasting;
        } else {
            options.startKey(new Object[]{"VPRO", avType.toString(), now.getTimeInMillis()});
            options.endKey((new Object[]{"VPRO", avType.toString()}));
            view = couchdbViewReplayableRrogramsByAvtype;
        }
        return createCouchdbViewUrl(view, options);
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


    /**
     * Group calls
     */

    @Override
    /**
     * Returns a group, optionally with members.
     * If addMembers=true, and memberTypes is null or empty, all members are returned.
     * If addMembers=true, and specific memberTypes are indicated, only members of those indicated MediaObjectTypes are returned.
     * If addSubgroups=true subgroups are also returned.
     */
    public Group getGroup(Long id, boolean addMembers, boolean addEpisodes, boolean addSubgroups, List<MediaObjectType> memberTypesFilter) {
        String urn = MediaUtil.createUrnFromId(MediaObjectType.group, id);
        try {
            ResponseEntity<Group> groupResponseEntity = restTemplate.getForEntity("{base}/{urn}", Group.class, couchdbUrlprovider.getUrl(), urn);
            Group group = groupResponseEntity.getBody();
            if (addMembers) {

                group.getMembers().addAll(getMediaForGroup(group, memberTypesFilter)); //MediaObjectType.group, MediaObjectType.program, MediaObjectType.segment
            }
            if (addEpisodes) {
                group.getMembers().addAll(getEpisodesForGroup(group)); //MediaObjectType.group, MediaObjectType.program, MediaObjectType.segment
            }
            if (addSubgroups) {
                for (Long subgroupId : getSubgroupIds(group)) {
                    group.getMembers().add(getGroup(subgroupId, addMembers, addEpisodes, addSubgroups, memberTypesFilter));
                }
            }
            return group;
        } catch (HttpServerErrorException e) {
            throw new ServerErrorException(couchdbUrlprovider.getUrl() + ": " + e.getMessage(), e);
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

    /**
     * Segment calls
     */

    @Override
    public Segment getSegment(Long id) {
        String urn = MediaUtil.createUrnFromId(MediaObjectType.segment, id);
        try {
            ResponseEntity<Segment> segmentResponseEntity = restTemplate.getForEntity("{base}/{urn}", Segment.class, couchdbUrlprovider.getUrl(), urn);
            return segmentResponseEntity.getBody();
        } catch (HttpServerErrorException e) {
            throw new ServerErrorException(couchdbUrlprovider.getUrl() + ": " + e.getMessage(), e);
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

    @Override
    public Iterator<MediaSearchResultItem> getProfile(String profileName) {
        try {
            Profile profile = profileService.getProfile(profileName);

            // Implemented with couchdb now
            // This works, but the location formats for segments are not in couchdb
            // So it will have to request the program for every segment to fill the result appropriately.
            // This may be a bit expensive. It probably can also be implemented with solr.
            return getProfileWithCouchdb(profile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Iterator<MediaSearchResultItem> getProfileWithCouchdb(Profile profile) throws IOException {
        String urn = profile.getArchiveUrn();
        Options options = new Options();
        options.reduce(false);
        options.descending(false);
        options.includeDocs(true);

        URL couchdb = new URL(couchdbUrlprovider.getUrl()
            + "/_design/media/_view/by-ancestor-and-type?reduce=false&startkey=[\"" + urn + "\"]&endkey=[\"" + urn + "\",{}]&inclusive_end=true&include_docs=true");
        InputStream inputStream = couchdb.openStream();

        SearchFilter searchFilter = profile.createFilterQuery();

        if (searchFilter instanceof MediaSearchFilter) {
            MediaSearchFilter mediaSearchFilter = (MediaSearchFilter) searchFilter;
            Iterator<MediaObject> iterator = new FilteringIterator<MediaObject>(new MediaObjectIterator(new CouchdbViewIterator(inputStream)), mediaSearchFilter.getPredicate());
            return new WrappedIterator<MediaObject, MediaSearchResultItem>(iterator) {
                @Override
                public MediaSearchResultItem next() {
                    return conversionService.convert(
                            wrapped.next(),
                            MediaSearchResultItem.class);
                }
            };
        }
        else {
            throw new IllegalArgumentException("Profile is not a media profile.");
        }
    }

    public void setGlobalMaxResult(int globalMaxResult) {
        this.globalMaxResult = globalMaxResult;
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
            viewResult = getViewResult(group.getUrn(), "media/by-group");
            for (ValueRow<Map> row : viewResult.getRows()) {
                String urn = row.getId();
                if (memberTypesFilter == null || memberTypesFilter.isEmpty() || memberTypesFilter.contains(MediaUtil.getMediaType(urn))) {
                    mediaIds.add(urn);
                }
            }
            mediaObjects = getMediaObjects(mediaIds);

            Collections.sort(mediaObjects, group.isIsOrdered() ? new SortInGroupByOrderComparator(group) : new SortInGroupByDateComparator(group));
        }
        return mediaObjects;
    }

    /**
     * get the episodes of a group.
     * Optionally filter on specific types of members.
     * If the filter is null or empty, any member is added to the returned result.
     *
     * @param group The group of which we want to get the members
     * @return
     */
    private List<Program> getEpisodesForGroup(final Group group) {
        List<Program> programs = new ArrayList<Program>();
        ViewResult<Map> viewResult;
        List<String> mediaIds = new ArrayList<String>();

        if (group != null) {
            viewResult = getViewResult(group.getUrn(), "media/episodes-by-group");
            for (ValueRow<Map> row : viewResult.getRows()) {
                String urn = row.getId();
                if (MediaObjectType.program.name().equals(MediaUtil.getMediaType(urn).name())) {
                    mediaIds.add(urn);
                }
            }
            for (MediaObject mediaObject : getMediaObjects(mediaIds)) {
                if (mediaObject instanceof Program) {
                    programs.add((Program) mediaObject);
                }
            }
            // Collections.sort(programs, group.isIsOrdered() ? new SortEpisodesInGroupByOrderComparator(group) : new SortEpisodesInGroupByDateComparator(group));
            Collections.sort(programs, new SortEpisodesByDateComparator());
        }
        return programs;
    }

    private List<Long> getSubgroupIds(final Group group) {
        List<Long> subgroupIds = new ArrayList<Long>();
        if (group != null) {
            ViewResult<Map> viewResult = getViewResult(group.getUrn(), "media/by-group");
            for (ValueRow<Map> row : viewResult.getRows()) {
                String urn = row.getId();
                if (MediaObjectType.group.name().equals(MediaUtil.getMediaType(urn).name())) {
                    Long subgroupId = MediaUtil.getMediaId(MediaObjectType.group, urn);
                    subgroupIds.add(subgroupId);
                }
            }
        }
        return subgroupIds;
    }

    /**
     * Get a list of mediaobjects from a list of ids
     *
     * @param ids
     * @return
     */
    private List<MediaObject> getMediaObjects(List<String> ids) {
        List<MediaObject> mediaObjects = new ArrayList<MediaObject>(ids.size());
        // Note: the direct view variant does not work with couchdb 1.0.2 time to phase out this library
        ViewAndDocumentsResult<Map, Map> progs = couchDbMediaServer.queryDocumentsByKeys(Map.class, Map.class, ids, new Options(), new JSONParser());
        for (ValueAndDocumentRow<Map, Map> row : progs.getRows()) {
            Map m = row.getDocument();
            String urn = (String) m.get("_id");
            MediaObject mediaObject = MediaMapper.convert(urn, m);
            if (mediaObject != null) {
                mediaObjects.add(mediaObject);
            } else {
                LOG.error("Unknown mediatype for urn : " + urn);
            }
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

    private static final class SortEpisodesInGroupByOrderComparator implements Comparator<Program>, Serializable {
        private static final long serialVersionUID = 23450383305L;

        protected final Group group;

        public SortEpisodesInGroupByOrderComparator(Group group) {
            this.group = group;
        }

        @Override
        public int compare(Program media, Program media1) {
            return getMemberRef(media.getEpisodeOf(), group).getIndex().compareTo(getMemberRef(media1.getEpisodeOf(), group).getIndex());
        }
    }

    private static final class SortEpisodesInGroupByDateComparator implements Comparator<Program>, Serializable {
        private static final long serialVersionUID = 23450389305L;
        protected final Group group;

        public SortEpisodesInGroupByDateComparator(Group group) {
            this.group = group;
        }

        @Override
        public int compare(Program media, Program media1) {
            return -(getMemberRef(media.getEpisodeOf(), group).getAdded().compareTo(getMemberRef(media1.getEpisodeOf(), group).getAdded()));
        }
    }

    private static final class SortEpisodesByDateComparator implements Comparator<Program>, Serializable {
        private static final long serialVersionUID = 23450389305L;

        @Override
        public int compare(Program media, Program media1) {
            if (media.getSortDate()!=null) {
                if (media1.getSortDate()!=null) {
                    return media.getSortDate().compareTo(media1.getSortDate());
                } else {
                    return 1;
                }
            } else {
                return -1;
            }
        }
    }

    private static MemberRef getMemberRef(List<MemberRef> refs, MediaObject parent) {
        MemberRef foundRef = null;
        for (MemberRef ref : refs) {
            if (ref.getUrnRef().equals(parent.getUrn())) {
                foundRef = ref;
                break;
            }
        }
        return foundRef;
    }
}
