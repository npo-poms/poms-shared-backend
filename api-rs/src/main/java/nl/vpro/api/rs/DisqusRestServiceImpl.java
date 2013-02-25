package nl.vpro.api.rs;

import net.sf.ehcache.Cache;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;

/**
 * @author Michiel Meeuwissen
 * @since 1.4
 */
@Controller
public class DisqusRestServiceImpl implements DisqusRestService{

	//@Resource(name = "threadDetailsCache")
	Cache cache;

	@Override
	public JSONArray getDetails(String... ids) {
		JSONArray array = new JSONArray();
		for (String id : ids) {
			array.put(getDetailsForOneThread(id));
		}
		return array;
	}


	private JSONObject getDetailsForOneThread(String id) {
		return new JSONObject();
	}

}
