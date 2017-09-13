package nl.vpro.pages.domain.es;

/**
 * @author Michiel Meeuwissen
 * @since 5.1
 */
public class ApiPageUpdatesIndex {

    public static String NAME = "pageupdates";

    public static String TYPE = "pageupdate";

    public static String DELETEDTYPE = "deletedpageupdate";


    public static String source() {
        return ApiPagesIndex.source("es5/setting/apipages.json");
    }



    public static String mappingSource() {
        return ApiPagesIndex.source("es5/mapping/pageupdate.json");
    }

    public static String deletedMappingSource() {
        return ApiPagesIndex.source("es5/mapping/deletedpageupdate.json");
    }

}
