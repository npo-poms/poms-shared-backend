package nl.vpro.api.rs.v3.validation;

import javax.ws.rs.ext.Provider;

import nl.vpro.domain.api.page.PageForm;

/**
 * @author Michiel Meeuwissen
 * @since 3.4
 */
@Provider
public class PageFormValidatingReader extends AbstractValidatingReader<PageForm> {

    @Override
    Class<PageForm> getClassToRead() {
        return PageForm.class;

    }
}
