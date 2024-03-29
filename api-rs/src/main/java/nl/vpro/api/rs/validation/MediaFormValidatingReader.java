package nl.vpro.api.rs.validation;

import jakarta.ws.rs.ext.Provider;

import nl.vpro.domain.Xmlns;
import nl.vpro.domain.api.media.MediaForm;

/**
 * @author Michiel Meeuwissen
 * @since 3.4
 */
@Provider
public class MediaFormValidatingReader extends AbstractValidatingReader<MediaForm> {

    public MediaFormValidatingReader() {
        super(MediaForm.class, Xmlns.API_NAMESPACE);
    }
}
