package nl.vpro.api.cors;

/**
 * User: ricojansen
 * Date: 27-04-2012
 * Time: 15:16
 */
public interface CorsPolicy {
    public boolean allowedOrigin(String origin);

    public boolean allowedOriginAndMethod(String origin, String method);

    public boolean isEnabled();
}
