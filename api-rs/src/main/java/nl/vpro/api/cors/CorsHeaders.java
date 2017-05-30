package nl.vpro.api.cors;

/**
 * @author ricojansen
 */
public class CorsHeaders {


    // Response Header Values
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_VALUE = "http://*.vpro.nl";
    public static final String ACCESS_CONTROL_ALLOW_METHODS_VALUE = "GET, HEAD, OPTIONS, POST, DELETE, PUT";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS_VALUE = "accept, authorization, content-type, cookie, origin, x-http-method-override, x-requested-with, x-npo-date, x-npo-mid, x-npo-url, x-origin";
}
