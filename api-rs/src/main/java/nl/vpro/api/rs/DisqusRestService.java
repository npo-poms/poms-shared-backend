package nl.vpro.api.rs;

import org.json.JSONArray;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Michiel Meeuwissen
 * @since 1.4
 */
@Path(DisqusRestService.PATH)
public interface DisqusRestService {
	public static final String PATH = "disqus";


	@GET
	@Path("details/{ids}")
	@Produces(MediaType.APPLICATION_JSON)
	public JSONArray getDetails(@PathParam("ids") String ... ids);


}
