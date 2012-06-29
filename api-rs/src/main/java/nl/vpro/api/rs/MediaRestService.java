package nl.vpro.api.rs;

import nl.vpro.api.domain.media.Group;
import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.transfer.MediaSearchResult;
import nl.vpro.api.transfer.MediaSearchSuggestions;
import nl.vpro.transfer.ugc.annotation.Annotations;
import nl.vpro.util.rs.error.NotFoundException;
import nl.vpro.util.rs.error.ServerErrorException;

import javax.ws.rs.*;

/**
 * Public REST API to api.service.MediaService
 * @author : maarten hogendoorn
 */
public interface MediaRestService {
    public Program getProgram(String urn);

    public Annotations getAnnotationsForProgram(String urn);

    public Group getGroup(String urn, boolean addMembers) throws ServerErrorException, NotFoundException;

    public Segment getSegment(String urn) throws ServerErrorException, NotFoundException;

    /**
     * @param queryString zoekterm volgens Lucene
     * @param tags spatie-gescheiden tags
     * @param maxResult maximum aantal resultaten, meer dan 50 wordt nog niet ondersteund
     * @param offset te beginnen bij dit resultaat
     * @return de gevonden media in een MediaSearchResult
     */
    public MediaSearchResult search(String queryString, String tags, Integer maxResult, Integer offset);

    /**
     * Only the first parameter is obligatory.
     * @param profileName, must be filled. Currently supported profiles: "woord" and "vpro"
     * @param queryString
     * @param tags
     * @param maxResult
     * @param offset
     * @return
     * @throw new IllegalArgumentException if (StringUtils.isEmpty(profileName))
     */
    public MediaSearchResult searchWithProfile(String profileName, String queryString, String tags, Integer maxResult, Integer offset);

    public MediaSearchSuggestions searchSuggestions(String queryString, String tags);

    public MediaSearchSuggestions searchSuggestionsWithProfile(String queryString, String tags, String profileName);
}
