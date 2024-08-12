package nl.vpro.api.cors;

/**
 * @author rico jansen
 */
public interface CorsPolicy {

    boolean allowedOriginAndMethod(String origin, String method);

    boolean isEnabled();
}
