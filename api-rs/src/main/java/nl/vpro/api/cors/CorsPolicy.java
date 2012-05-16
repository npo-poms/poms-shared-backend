package nl.vpro.api.cors;

/**
 * Created with IntelliJ IDEA.
 * User: ricojansen
 * Date: 27-04-2012
 * Time: 15:16
 * To change this template use File | Settings | File Templates.
 */
public interface CorsPolicy {
    public boolean allowedOrigin(String origin);

    public boolean allowedOriginAndMethod(String origin, String method);

    public boolean isEnabled();
}
