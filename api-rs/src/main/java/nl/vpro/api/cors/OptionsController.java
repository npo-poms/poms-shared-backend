package nl.vpro.api.cors;

/**
 * User: ricojansen
 * Date: 01-05-2012
 */

import javax.ws.rs.OPTIONS;

//@Controller
//@Path("{path : .*}")
public class OptionsController {
    @OPTIONS
    public void options() {
        // For CORS support
    }
}
