package nl.vpro.api.rs;

import nl.vpro.api.service.UgcService;
import nl.vpro.domain.ugc.playerconfiguration.PlayerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * Date: 1-5-12
 * Time: 11:25
 *
 * @author Ernst Bunders
 */
@Path("/ugc")
@Controller
public class UGC {

    @Autowired
    private UgcService ugcService;

    @GET
    @Path("playerconfiguration/{urn}")
    public PlayerConfiguration getPlayerConfiguration(@PathParam("urn") String id) {
        return ugcService.getPlayerConfiguration(id);
    }
}
