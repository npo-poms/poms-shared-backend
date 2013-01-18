package nl.vpro.api.service;

/**
 * Date: 19-3-12
 * Time: 11:50
 *
 * @author Ernst Bunders
 *
 * Tries to find a named profile. If that fales, return the default profile
 */
public interface ProfileService {
    Profile getProfile(String name);
}
