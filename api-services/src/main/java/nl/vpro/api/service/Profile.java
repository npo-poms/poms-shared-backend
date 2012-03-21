package nl.vpro.api.service;

import nl.vpro.api.service.querybuilder.BooleanOp;
import nl.vpro.api.service.querybuilder.MediaSearchQuery;
import nl.vpro.api.service.querybuilder.BooleanMediaSearchQuery;
import nl.vpro.api.service.querybuilder.MediaSearchQueryList;
import nl.vpro.domain.media.AVFileFormat;
import nl.vpro.domain.media.AVType;
import nl.vpro.domain.media.search.MediaType;

/**
 * Date: 19-3-12
 * Time: 11:50
 *
 * @author Ernst Bunders
 */
public enum Profile {
    WOORD("woord", "Woord.nl") {
        public MediaSearchQuery createSearchQuery() {

            return new MediaSearchQueryList(BooleanOp.OR)
                .addQuery(new BooleanMediaSearchQuery(BooleanOp.AND) /*program*/
                    .addAvType(AVType.AUDIO)
                    .addDescendant(getArchiveUrn())
                    .addMediaType(MediaType.BROADCAST)
                    .addLocationFormat(AVFileFormat.MP3))
                .addQuery(new BooleanMediaSearchQuery(BooleanOp.AND) /*any type of group*/
                    .addDescendant(getArchiveUrn())
                    .setDocumentType(BooleanMediaSearchQuery.DOCUMENT_TYPE_GROUP)
                );
        }
    },

    DEFAULT("", "") {
        public MediaSearchQuery createSearchQuery() {
            return new BooleanMediaSearchQuery(BooleanOp.AND);
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

    abstract public MediaSearchQuery createSearchQuery();

}
