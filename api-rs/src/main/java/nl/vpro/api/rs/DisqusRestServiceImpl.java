package nl.vpro.api.rs;

import nl.vpro.api.service.DisqusService;
import nl.vpro.api.transfer.DisqusThreadInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Michiel Meeuwissen
 * @author Ernst Bunders
 * @since 1.4
 */
@Controller
public class DisqusRestServiceImpl implements DisqusRestService {

    @Autowired
    private DisqusService disqusService;


    @Override
    public Map<String, DisqusThreadInfo> getDetails(final String siteName, final String... ids) {
        Map<String, DisqusThreadInfo> result = new HashMap<String, DisqusThreadInfo>();

        for (String id : ids) {
            result.put(id, disqusService.getThreadInfo(siteName, id));
        }
        return result;
    }
}
