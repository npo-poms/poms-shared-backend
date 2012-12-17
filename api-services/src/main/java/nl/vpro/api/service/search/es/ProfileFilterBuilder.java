package nl.vpro.api.service.search.es;

import nl.vpro.api.domain.media.AvFileFormat;
import nl.vpro.api.domain.media.AvType;
import nl.vpro.api.domain.media.search.MediaType;
import nl.vpro.api.service.Profile;
import nl.vpro.api.service.search.fiterbuilder.BooleanOp;
import nl.vpro.api.service.search.fiterbuilder.DocumentSearchFilter;
import nl.vpro.api.service.search.fiterbuilder.SearchFilter;
import nl.vpro.api.service.search.fiterbuilder.SearchFilterList;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BaseFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.query.FilterBuilders.*;

/**
 * This class can create an ElasticSearch query filter from an SearchFilter
 * instance.
 * User: ernst
 * Date: 10/2/12
 * Time: 12:23 PM
 */
public class ProfileFilterBuilder extends BaseFilterBuilder {
    private FilterBuilder result;

    /**
     * @param profile any profile.
     * @throws IllegalStateException when the profile is null or it's query filter is null
     */
    public ProfileFilterBuilder(Profile profile) throws IllegalStateException{
        if (profile == null || profile.createFilterQuery() == null) {
            throw new IllegalStateException("profile and filter query may not be null");
        }
        createFilter(profile.createFilterQuery());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return result.toXContent(builder, params);
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        // nothing
    }

    public void createFilter(SearchFilter mediaSearchQuery) {
        if (mediaSearchQuery instanceof SearchFilterList) {
            result = parseFilterList((SearchFilterList) mediaSearchQuery);
        }

        if (mediaSearchQuery instanceof DocumentSearchFilter) {
            result = parseBooleanFilter((DocumentSearchFilter) mediaSearchQuery);
        }
    }

    private FilterBuilder parseFilterList(SearchFilterList queryList) {
        final List<FilterBuilder> filterBuilderList = new ArrayList<FilterBuilder>();
        for (SearchFilter childQuery : queryList.getMediaSearchQueries()) {
            if (childQuery instanceof SearchFilterList) {
                filterBuilderList.add(parseFilterList((SearchFilterList) childQuery));
            } else if (childQuery instanceof DocumentSearchFilter) {
                // If this queryList is of type and, we can just add all the term filters
                // of the boolean query, and not wrap it in it's own and filter
                if (queryList.getBooleanOp() == BooleanOp.AND) {
                    //add filters to current and filter
                    filterBuilderList.addAll(gatherTermFilters((DocumentSearchFilter) childQuery));
                } else {
                    //or: create new nested and filter
                    filterBuilderList.add(parseBooleanFilter((DocumentSearchFilter) childQuery));
                }
            }
        }
        if (queryList.getBooleanOp() == BooleanOp.AND) {
            return andFilter(filterBuilderList.toArray(new FilterBuilder[filterBuilderList.size()]));
        }else /*OR*/{
            return orFilter(filterBuilderList.toArray(new FilterBuilder[filterBuilderList.size()]));
        }
    }


    private FilterBuilder parseBooleanFilter(DocumentSearchFilter documentSearchFilter) {
        final List<FilterBuilder> fbl = gatherTermFilters(documentSearchFilter);
        if (fbl == null) return null;

        return fbl.size() > 1 ?
                andFilter(fbl.toArray(new FilterBuilder[fbl.size()]))
                : fbl.get(0);
    }

    private List<FilterBuilder> gatherTermFilters(DocumentSearchFilter documentSearchFilter) {
        final List<FilterBuilder> fbl = new ArrayList<FilterBuilder>();
        for (MediaType mediaType : documentSearchFilter.getMediaTypes()) {
            fbl.add(termFilter("type", mediaType.name()));
        }

        for (AvFileFormat fileFormat : documentSearchFilter.getLocationFormats()) {
            fbl.add(termFilter("locations.avAttributes.avFileFormat", fileFormat.name()));
        }

        for (String broadcaster : documentSearchFilter.getBroadcasters()) {
            fbl.add(termFilter("broadcasters", broadcaster.toLowerCase()));
        }

        for (AvType avType : documentSearchFilter.getAvTypes()) {
            fbl.add(termFilter("avType", avType.name()));
        }

        for (String descendant : documentSearchFilter.getDescendants()) {
            fbl.add(termFilter("descendantOf", descendant));
        }

        if (StringUtils.isNotBlank(documentSearchFilter.getMainTitle())) {
            fbl.add(termFilter("titles", documentSearchFilter.getMainTitle()));
        }

        if (StringUtils.isNotBlank(documentSearchFilter.getDocumentType())) {
            fbl.add(termFilter("_type", documentSearchFilter.getDocumentType()));
        }
        if (fbl.size() == 0) {
            return null;
        }
        return fbl;
    }
}
