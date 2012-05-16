package nl.vpro.api.service;

import nl.vpro.api.domain.media.AvFileFormat;
import nl.vpro.api.domain.media.AvType;
import nl.vpro.api.service.searchfilterbuilder.BooleanOp;
import nl.vpro.api.service.searchfilterbuilder.DocumentSearchFilter;
import nl.vpro.api.service.searchfilterbuilder.SearchFilter;
import nl.vpro.api.service.searchfilterbuilder.SearchFilterList;


/**
 * Date: 19-3-12
 * Time: 11:50
 *
 * @author Ernst Bunders
 */
public enum Profile {
    WOORD("woord", "Woord.nl") {
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
    },

    DEFAULT("", "") {
        public SearchFilter createFilterQuery() {
            return null;
        }
    };

    private final String name;

    private final String archiveName;

    private String archiveUrn;

    Profile(String name, String archiveName) {
        this.name = name;
        this.archiveName = archiveName;
    }

    public String getName() {
        return name;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveUrn(String archiveUrn) {
        this.archiveUrn = archiveUrn;
    }

    public String getArchiveUrn() {
        return archiveUrn;
    }

    abstract public SearchFilter createFilterQuery();

}
