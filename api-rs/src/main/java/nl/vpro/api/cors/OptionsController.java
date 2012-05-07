package nl.vpro.api.cors;

/**
 * User: ricojansen
 * Date: 01-05-2012
 */

import org.springframework.stereotype.Controller;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;

//@Controller
//@Path("{path : .*}")
public class OptionsController {
    @OPTIONS
    public void options() {
        // For CORS support
    }
}
