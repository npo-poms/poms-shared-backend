package nl.vpro.api.service.search.es;

import nl.vpro.api.domain.media.AvFileFormat;
import nl.vpro.api.domain.media.AvType;
import nl.vpro.api.domain.media.search.MediaType;
import nl.vpro.api.service.Profile;
import nl.vpro.api.service.search.filterbuilder.*;
import org.apache.commons.lang.StringUtils;
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

        if (mediaSearchQuery instanceof MediaSearchFilter) {
            result = parseBooleanFilter((MediaSearchFilter) mediaSearchQuery);
        }

        if (mediaSearchQuery instanceof FieldFilter) {
            result = parseFieldFilter((FieldFilter) mediaSearchQuery);
        }

        if (mediaSearchQuery instanceof PrefixFieldFilter) {
            result = parsePrefixFilter((PrefixFieldFilter) mediaSearchQuery);
        }
    }

    private FilterBuilder parseFieldFilter(FieldFilter fieldFilter) {
        return termFilter(fieldFilter.getField(),fieldFilter.getValue());
    }

    private FilterBuilder parsePrefixFilter(PrefixFieldFilter prefixFieldFilter) {
        return prefixFilter(prefixFieldFilter.getField(), prefixFieldFilter.getValue());
    }

    private FilterBuilder parseFilterList(SearchFilterList searchFilterList) {
        final List<FilterBuilder> filterBuilderList = new ArrayList<FilterBuilder>();
        for (SearchFilter searchFilter : searchFilterList.getSearchFilters()) {
            if (searchFilter instanceof SearchFilterList) {
                filterBuilderList.add(parseFilterList((SearchFilterList) searchFilter));
            } else if (searchFilter instanceof MediaSearchFilter) {
                // If this searchFilterList is of type AND, we can just add all the term filters
                // of the boolean query, and not wrap it in its own AND-filter
                if (searchFilterList.getBooleanOp() == BooleanOp.AND) {
                    //add filters to current AND-filter
                    filterBuilderList.addAll(gatherTermFilters((MediaSearchFilter) searchFilter));
                } else {
                    //or: create new nested AND-filter
                    filterBuilderList.add(parseBooleanFilter((MediaSearchFilter) searchFilter));
                }
            }
        }
        if (searchFilterList.getBooleanOp() == BooleanOp.AND) {
            return andFilter(filterBuilderList.toArray(new FilterBuilder[filterBuilderList.size()]));
        }else /*OR*/{
            return orFilter(filterBuilderList.toArray(new FilterBuilder[filterBuilderList.size()]));
        }
    }


    private FilterBuilder parseBooleanFilter(MediaSearchFilter mediaSearchFilter) {
        final List<FilterBuilder> fbl = gatherTermFilters(mediaSearchFilter);
        if (fbl == null) return null;

        return fbl.size() > 1 ?
                andFilter(fbl.toArray(new FilterBuilder[fbl.size()]))
                : fbl.get(0);
    }

    private List<FilterBuilder> gatherTermFilters(MediaSearchFilter mediaSearchFilter) {
        final List<FilterBuilder> fbl = new ArrayList<FilterBuilder>();
        for (MediaType mediaType : mediaSearchFilter.getMediaTypes()) {
            fbl.add(termFilter("type", mediaType.name()));
        }

        for (AvFileFormat fileFormat : mediaSearchFilter.getLocationFormats()) {
            fbl.add(termFilter("locations.avAttributes.avFileFormat", fileFormat.name()));
        }

        for (String broadcaster : mediaSearchFilter.getBroadcasters()) {
            fbl.add(termFilter("broadcasters", broadcaster.toLowerCase()));
        }

        for (AvType avType : mediaSearchFilter.getAvTypes()) {
            fbl.add(termFilter("avType", avType.name()));
        }

        for (String descendant : mediaSearchFilter.getDescendants()) {
            fbl.add(termFilter("descendantOf", descendant));
        }

        if (StringUtils.isNotBlank(mediaSearchFilter.getMainTitle())) {
            fbl.add(termFilter("titles", mediaSearchFilter.getMainTitle()));
        }

        if (mediaSearchFilter.getDocumentType() != null) {
            fbl.add(termFilter("_type", mediaSearchFilter.getDocumentType()));
        }
        if (fbl.size() == 0) {
            return null;
        }
        return fbl;
    }
}
