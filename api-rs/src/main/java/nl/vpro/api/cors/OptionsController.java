package nl.vpro.api.cors;

/**
 * User: ricojansen
 * Date: 01-05-2012
 */

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;

import org.springframework.stereotype.Controller;

@Controller
@Path(OptionsController.PATH)
public class OptionsController {

    /**
     * Mention all RESTable URLs here. In this way, only RESTable Urls are intercepted,
     * such that /index.html remains accessible.
     */
    public static final String PATH =
        "{path : (/media" +
            "|/pages" +
            "|/profiles" +
            ")/.*}";

    @OPTIONS
    public String options() {
        return "ok";
        // For CORS support
    }

}
