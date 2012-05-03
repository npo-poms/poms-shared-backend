package nl.vpro.api.rs;

import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.service.MediaService;
import nl.vpro.api.service.UgcService;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.domain.ugc.playerconfiguration.PlayerConfiguration;
import nl.vpro.ugc.util.UrnUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
    Logger logger = LoggerFactory.getLogger(UGC.class);

    @Autowired
    private MediaService mediaService;
    @Autowired
    private UgcService ugcService;

    @GET
    @Path("playerconfiguration/{urn}")
    public PlayerConfiguration getPlayerConfiguration(@PathParam("urn") String id) {
        return ugcService.getPlayerConfiguration(id);
    }

    @POST
    @Path("playerconfiguration")
    public PlayerConfiguration insertPlayerConfiguration(PlayerConfiguration playerConfiguration) {
        return ugcService.insertPlayerConfiguration(playerConfiguration);
    }
}
