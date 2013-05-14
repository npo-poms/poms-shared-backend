package nl.vpro.api.rs;

import nl.vpro.api.domain.media.Segment;
import nl.vpro.api.domain.media.support.MediaObjectType;
import nl.vpro.api.domain.media.support.MediaUtil;
import nl.vpro.api.service.MediaService;
import nl.vpro.api.service.UgcService;
import nl.vpro.domain.ugc.annotation.Annotation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Date: 1-5-12
 * Time: 11:25
 *
 * @author Ernst Bunders
 */
@Controller
public class UGCRestServiceImpl implements UGCRestService {
    Logger logger = LoggerFactory.getLogger(UGCRestServiceImpl.class);

    @Autowired
    private MediaService mediaService;
    @Autowired
    private UgcService ugcService;


    @Override
	public Annotation getAnnotation(String id) {
        if (StringUtils.isNotEmpty(id)) {
            if (MediaUtil.isUrn(id)) {
                return mediaService.getProgramAnnotation(MediaUtil.getMediaId(MediaObjectType.program, id));
            } else {
                return ugcService.getAnnotation(id);
            }
        }
        return null;
    }

    @Override
	public Annotation getAnnotationByPart(String id) {
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


