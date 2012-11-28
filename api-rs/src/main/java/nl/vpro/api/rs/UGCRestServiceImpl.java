package nl.vpro.api.rs;

import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.service.MediaService;
import nl.vpro.api.service.UgcService;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.domain.ugc.playerconfiguration.PlayerConfiguration;
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
@Path(UGCRestServiceImpl.PATH)
@Controller
public class UGCRestServiceImpl implements UGCRestService {
    public static final String PATH = "ugc";
    Logger logger = LoggerFactory.getLogger(UGCRestServiceImpl.class);

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

    @GET
    @Path("annotation/{urn}")
    public Annotation getAnnotation(@PathParam("urn") String id) {
        if (StringUtils.isNotEmpty(id)) {
            if (MediaUtil.isUrn(id)) {
                return mediaService.getProgramAnnotation(MediaUtil.getMediaId(MediaObjectType.program, id));
            } else {
                return ugcService.getAnnotation(id);
            }
        }
        return null;
    }

    @GET
    @Path("annotation/bypart/{urn}")
    public Annotation getAnnotationByPart(@PathParam("urn") String id) {
        if (StringUtils.isNotEmpty(id)) {
            if (MediaUtil.isUrn(id)) {
                Segment segment = mediaService.getSegment(MediaUtil.getMediaId(MediaObjectType.segment, id));
                if (segment != null) {
                    String urn = segment.getUrnRef();
                    return mediaService.getProgramAnnotation(MediaUtil.getMediaId(MediaObjectType.program, urn));
                }
            } else {
                return ugcService.getAnnotiationByPart(id);
            }
        }
        return null;
    }
}


