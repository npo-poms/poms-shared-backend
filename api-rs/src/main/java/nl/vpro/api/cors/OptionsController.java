package nl.vpro.api.cors;

/**
 * User: ricojansen
 * Date: 01-05-2012
 */

import nl.vpro.api.rs.DisqusRestService;
import nl.vpro.api.rs.MediaRestService;
import nl.vpro.api.rs.SiteRestService;
import nl.vpro.api.rs.UGCRestService;
import org.springframework.stereotype.Controller;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;

@Controller
@Path(OptionsController.PATH)
public class OptionsController {

    /**
     * Mention all RESTable URLs here. In this way, only RESTable Urls are intercepted,
     * such that /index.html remains accessible.
     */
    public static final String PATH
            = "{path : (" + MediaRestService.PATH +
            "|" + UGCRestService.PATH +
            "|" + SiteRestService.PATH +
            "|" + DisqusRestService.PATH +
            ")/.*}";

    @OPTIONS
    public String options() {
        return "ok";
        // For CORS support
    }

}
