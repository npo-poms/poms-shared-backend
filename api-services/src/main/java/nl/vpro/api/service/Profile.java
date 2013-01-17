package nl.vpro.api.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.vpro.api.domain.media.AvFileFormat;
import nl.vpro.api.domain.media.AvType;
import nl.vpro.api.service.search.fiterbuilder.BooleanOp;
import nl.vpro.api.service.search.fiterbuilder.DocumentSearchFilter;
import nl.vpro.api.service.search.fiterbuilder.SearchFilter;
import nl.vpro.api.service.search.fiterbuilder.SearchFilterList;


/**
 * Date: 19-3-12
 *
 * @author Ernst Bunders
 */
public enum Profile {
    WOORD("woord", "Woord.nl", "woord") {
        @Override
        public SearchFilter createFilterQuery() {

            //The document should be a program or a segment
            SearchFilterList documentTypes = new SearchFilterList(BooleanOp.OR)
                .addQuery(new DocumentSearchFilter()
                        .setDocumentType("program"))
                .addQuery(new DocumentSearchFilter()
                        .setDocumentType("segment")
                );


            //the document must have an mp3, be of type audio and must be part of the given archive.
            SearchFilter contentRules = new DocumentSearchFilter() /*program*/
                .addAvType(AvType.AUDIO)
                .addDescendant(getArchiveUrn())
                .addLocationFormat(AvFileFormat.MP3);

            return new SearchFilterList(BooleanOp.AND)
                .addQuery(documentTypes)
                .addQuery(contentRules);
        }
        @Override
        public List<String> getSearchFields() {
            return new ArrayList<String>();
        }

        @Override
        public List<Float> getSearchBoosting() {
            return new ArrayList<Float>();
        }
    },

    VPRO("vpro", "", "") {
        @Override
        public SearchFilter createFilterQuery() {
            return new DocumentSearchFilter()
                .addBroadcaster("VPRO");
        }

        @Override
        public List<String> getSearchFields() {
            return new ArrayList<String>();
        }

        @Override
        public List<Float> getSearchBoosting() {
            return new ArrayList<Float>();
        }
    },

    WETENSCHAP24("wetenschap24", "", "w24") {
        @Override
        public SearchFilter createFilterQuery() {
            return null;
        }

        @Override
        public List<String> getSearchFields() {
            return (Arrays.asList("title", "subtitle", "summary", "body", "extra_field_nl_vpro_page_type","extra_field_nl_vpro_subsite", "persons", "genre", "keywords"));
        }

        @Override
        public List<Float> getSearchBoosting() {
            return Arrays.asList(3.0f, 2.0f, 1.0f, 1.5f, 2.0f, 2.0f, 1.0f, 1.0f, 1.0f);
        }
    },

    DEFAULT("", "", "") {
        @Override
        public SearchFilter createFilterQuery() {
            return new DocumentSearchFilter()
                .addBroadcaster("VPRO");
        }
        @Override
        public List<String> getSearchFields() {
            return new ArrayList<String>();
        }

        @Override
        public List<Float> getSearchBoosting() {
            return new ArrayList<Float>();
        }
    };

    private final String name;

    private final String archiveName;

    private final String indexName;

    private String archiveUrn;

    Profile(String name, String archiveName, String indexName) {
        this.name = name;
        this.archiveName = archiveName;
        this.indexName = indexName;
    }

    public String getName() {
        return name;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public String getIndexName() {
        return indexName;
    }
    // a modifiable enum? odd...
    public void setArchiveUrn(String archiveUrn) {
        this.archiveUrn = archiveUrn;
    }

    public String getArchiveUrn() {
        return archiveUrn;
    }

    abstract public List<String> getSearchFields();

    abstract public List<Float> getSearchBoosting();

    abstract public SearchFilter createFilterQuery();

}
