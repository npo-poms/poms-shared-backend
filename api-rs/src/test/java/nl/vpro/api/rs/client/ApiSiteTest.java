/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.client;

import nl.vpro.api.transfer.GenericSearchResult;
import nl.vpro.api.transfer.GenericSearchResultItem;
import nl.vpro.api.transfer.SearchSuggestion;
import nl.vpro.api.transfer.SearchSuggestions;

import java.io.IOException;

/**
 * User: rico
 * Date: 06/12/2012
 */
public class ApiSiteTest {

    public static void main(String[] args) throws IOException {

        ApiClient client = new ApiClient("http://disaster.vpro.nl:8080/", 10000);
        SearchSuggestions suggestions = client.getSiteRestService().searchSuggestions("wetenschap24","sta",null);
        for (SearchSuggestion suggestion : suggestions.getSuggestions()) {
            System.out.println("Suggestion "+suggestion);
        }

        GenericSearchResult result=client.getSiteRestService().search("wetenschap24","vliegtuig",0,10,null,null,null,null);
        System.out.println("Found: "+result.getNumFound());
        for (GenericSearchResultItem item : result.getSearchResultItems()) {
            System.out.println("Title "+item.getResult().get("title"));

        }
    }

}
