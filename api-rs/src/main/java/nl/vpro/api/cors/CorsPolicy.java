package nl.vpro.api.cors;

/**
 * User: ricojansen
 * Date: 27-04-2012
 * Time: 15:16
 */
public interface CorsPolicy {

    boolean allowedOriginAndMethod(String origin, String method);

    boolean isEnabled();
}
