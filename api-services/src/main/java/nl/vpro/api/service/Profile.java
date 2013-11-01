package nl.vpro.api.service;

import nl.vpro.api.domain.media.AvFileFormat;
import nl.vpro.api.domain.media.AvType;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.service.search.filterbuilder.*;

import java.util.*;


/**
 * Date: 19-3-12
 *
 * @author Ernst Bunders
 */
public enum Profile {

    GESCHIEDENIS24("geschiedenis24", "", "g24") {
        @Override
        public SearchFilter createFilterQuery() {
            return new FieldFilter("brand_title", "geschiedenis");
        }

        @Override
        public List<String> getSearchFields() {
            return Arrays.asList("title", "subtitle", "summary", "body", "extra_field_nl_vpro_page_type", "extra_field_nl_vpro_subsite", "persons", "genre", "keywords");
        }

        @Override
        public List<Float> getSearchBoosting() {
            return Arrays.asList(3.0f, 2.0f, 1.0f, 1.5f, 2.0f, 2.0f, 1.0f, 1.0f, 1.0f);
        }

        @Override
        public String getScoreField() {
            return null;
        }

        @Override
        public Map<String, Float> getScoreTable() {
            return null;
        }
    },

    HUMAN("human", "", "") {
        @Override
        public SearchFilter createFilterQuery() {
            return new SearchFilterList(BooleanOp.AND)
                .add(new MediaSearchFilter().addBroadcaster("HUMAN"))
                .add(new MediaHasImageFilter("images.urn", "urn"))
                .add(new MediaHasLocationFilter("locations.urn", "urn"));
        }

        @Override
        public List<String> getSearchFields() {
            return new ArrayList<String>();
        }

        @Override
        public List<Float> getSearchBoosting() {
            return new ArrayList<Float>();
        }

        @Override
        public String getScoreField() {
            return null;
        }

        @Override
        public Map<String, Float> getScoreTable() {
            return null;
        }
    },

    WETENSCHAP24("wetenschap24", "", "w24") {
        @Override
        public SearchFilter createFilterQuery() {
            return new FieldFilter("brand_title", "wetenschap");
        }

        @Override
        public List<String> getSearchFields() {
            return Arrays.asList("title", "subtitle", "summary", "body", "extra_field_nl_vpro_page_type", "extra_field_nl_vpro_subsite", "persons", "genre", "keywords");
        }

        @Override
        public List<Float> getSearchBoosting() {
            return Arrays.asList(3.0f, 2.0f, 1.0f, 1.5f, 2.0f, 2.0f, 1.0f, 1.0f, 1.0f);
        }

        @Override
        public String getScoreField() {
            return "extra_field_nl_vpro_page_type";
        }

        @Override
        public Map<String, Float> getScoreTable() {
            Map<String, Float> scoreTable = new HashMap<String, Float>();
            scoreTable.put("Specials", 5.0f);
            scoreTable.put("Home", 3.0f);
            scoreTable.put("Video", 1.5f);
            scoreTable.put("Audio", 1.3f);
            scoreTable.put("Artikel", 1.0f);

            return scoreTable;
        }
    },

    WOORD("woord", "Woord.nl", "woord") {
        @Override
        public SearchFilter createFilterQuery() {

            //The document should be a program or a segment
            SearchFilterList documentTypes = new SearchFilterList(BooleanOp.OR)
                    .add(new MediaSearchFilter()
                            .setDocumentType(MediaObjectType.program))
                    .add(new MediaSearchFilter()
                            .setDocumentType(MediaObjectType.segment)
                );


            //the document must have an mp3, be of type audio and must be part of the given archive.
            SearchFilter contentRules = new MediaSearchFilter() /*program*/
                    .addAvType(AvType.AUDIO)
                    .addDescendant(getArchiveUrn())
                    .addLocationFormat(AvFileFormat.MP3);

            return new SearchFilterList(BooleanOp.AND)
                    .add(documentTypes)
                    .add(contentRules);
        }

			@Override
			public List<String> getSearchFields() {
				return Arrays.asList("title", "subTitle", "description", "programTitles", "genres", "tags", "broadcasters");
			}

			@Override
			public List<Float> getSearchBoosting() {
				return Arrays.asList(3.0f, 2.0f, 1.5f, 2.0f, 1.0f, 1.0f, 2.0f);
			}

        @Override
        public String getScoreField() {
            return null;
        }

        @Override
        public Map<String, Float> getScoreTable() {
            return null;
        }
    },

    VPRO("vpro", "", "") {
        @Override
        public SearchFilter createFilterQuery() {
            return new SearchFilterList(BooleanOp.AND)
                .add(new MediaSearchFilter().addBroadcaster("VPRO"))
                .add(new MediaHasImageFilter("images.urn", "urn"))
                .add(new MediaHasLocationFilter("locations.urn", "urn"));
        }

        @Override
        public List<String> getSearchFields() {
            return new ArrayList<String>();
        }

        @Override
        public List<Float> getSearchBoosting() {
            return new ArrayList<Float>();
        }

        @Override
        public String getScoreField() {
            return null;
        }

        @Override
        public Map<String, Float> getScoreTable() {
            return null;
        }
    },

    DEFAULT("", "", "") {
        @Override
        public SearchFilter createFilterQuery() {
            return new MediaSearchFilter()
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

        @Override
        public String getScoreField() {
            return null;
        }

        @Override
        public Map<String, Float> getScoreTable() {
            return null;
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

    abstract public String getScoreField();

    abstract public Map<String, Float> getScoreTable();

}
