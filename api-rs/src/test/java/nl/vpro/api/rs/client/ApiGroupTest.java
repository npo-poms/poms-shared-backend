/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.client;

import nl.vpro.api.domain.media.Group;

import java.io.IOException;

/**
 * User: rico
 * Date: 07/12/2012
 */
public class ApiGroupTest {
    public static void main(String args[]) throws IOException {
        ApiClient client = new ApiClient("http://disaster.vpro.nl:8080/", 10000);

        Group group = client.getMediaRestService().getGroup("urn:vpro:media:group:13551591", false, true, false, null);
        System.out.println("Group "+group.getMembers());
    }
}
