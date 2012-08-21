package nl.vpro.api.cors;

/**
 * User: ricojansen
 * Date: 27-04-2012
 */
public class CorsHeaders {
    // Request Headers
    public static final String ORIGIN = "Origin";
    public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    // Response Headers
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    // Response Header Values
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_VALUE = "http://*.vpro.nl";
    public static final String ACCESS_CONTROL_ALLOW_METHODS_VALUE = "GET, HEAD, OPTIONS, POST";
}
