package nl.vpro.api.cors;

/**
 * @author ricojansen
 */
public class CorsHeaders {
    // Request Headers

    /**
     * @deprecated Use org.springframework.http.HttpHeaders#ORIGIN instead
     */
    @Deprecated
    public static final String ORIGIN = "Origin";

    /**
     * @deprecated Use org.springframework.http.HttpHeaders#ACCESS_CONTROL_REQUEST_METHOD instead
     */
    @Deprecated
    public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

    /**
     * @deprecated Use org.springframework.http.HttpHeaders#ACCESS_CONTROL_REQUEST_HEADERS instead
     */
    @Deprecated
    public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    // Response Headers

    /**
     * @deprecated Use org.springframework.http.HttpHeaders#ACCESS_CONTROL_ALLOW_ORIGIN instead
     */
    @Deprecated
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    /**
     * @deprecated Use org.springframework.http.HttpHeaders#ACCESS_CONTROL_ALLOW_CREDENTIALS instead
     */
    @Deprecated
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    /**
     * @deprecated Use org.springframework.http.HttpHeaders#ACCESS_CONTROL_EXPOSE_HEADERS instead
     */
    @Deprecated
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    /**
     * @deprecated Use org.springframework.http.HttpHeaders#ACCESS_CONTROL_MAX_AGE instead
     */
    @Deprecated
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    /**
     * @deprecated Use org.springframework.http.HttpHeaders#ACCESS_CONTROL_ALLOW_METHODS instead
     */
    @Deprecated
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    /**
     * @deprecated Use org.springframework.http.HttpHeaders#ACCESS_CONTROL_ALLOW_HEADERS instead
     */
    @Deprecated
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    // Response Header Values
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_VALUE = "http://*.vpro.nl";
    public static final String ACCESS_CONTROL_ALLOW_METHODS_VALUE = "GET, HEAD, OPTIONS, POST, DELETE, PUT";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS_VALUE = "accept, authorization, content-type, cookie, origin, x-http-method-override, x-requested-with, x-npo-date, x-npo-mid, x-npo-url, x-origin";
}
