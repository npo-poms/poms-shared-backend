package nl.vpro.api.rs.validation;

import jakarta.ws.rs.ext.Provider;

import nl.vpro.api.util.ApiMappings;
import nl.vpro.domain.Xmlns;
import nl.vpro.domain.api.page.PageForm;

/**
 * @author Michiel Meeuwissen
 * @since 3.4
 */
@Provider
public class PageFormValidatingReader extends AbstractValidatingReader<PageForm> {

    public PageFormValidatingReader() {
        super(PageForm.class, Xmlns.API_NAMESPACE);
    }

    public PageFormValidatingReader(ApiMappings apiMappings) {
        super(PageForm.class, Xmlns.API_NAMESPACE, apiMappings);
    }
}
