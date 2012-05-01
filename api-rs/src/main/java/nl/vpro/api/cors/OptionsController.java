package nl.vpro.api.cors;

/**
 * Created with IntelliJ IDEA.
 * User: ricojansen
 * Date: 01-05-2012
 * Time: 16:00
 * To change this template use File | Settings | File Templates.
 */

import org.springframework.stereotype.Controller;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;

@Controller
@Path("{path : .*}")
public class OptionsController {
    @OPTIONS
    public void options() {
        // For CORS support
    }
}
