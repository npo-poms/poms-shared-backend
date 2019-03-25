package nl.vpro.api.rs.interceptors;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import nl.vpro.i18n.Locales;

/**
 * @author Michiel Meeuwissen
 * @since 4.9
 */
@Provider
@PreMatching
public class SetDefaultLocale implements ContainerRequestFilter  {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        List<Locale> acceptableLanguages = requestContext.getAcceptableLanguages();
        if (acceptableLanguages != null && ! acceptableLanguages.isEmpty()) {
            Locales.setDefault(acceptableLanguages.get(0));
        } else {
            Locales.resetDefault();
        }
    }

}
