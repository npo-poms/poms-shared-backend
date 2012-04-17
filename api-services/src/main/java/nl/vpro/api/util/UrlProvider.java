/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.util;

/**
 * User: rico
 * Date: 05/04/2012
 */
public class UrlProvider {
    String host;
    int port;
    String uri;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUrl() {
        StringBuffer buffer = new StringBuffer("http://");
        buffer.append(host);
        if (port > 0 && port != 80) {
            buffer.append(":").append(port);
        }
        buffer.append("/").append(uri);
        return buffer.toString();
    }
}
