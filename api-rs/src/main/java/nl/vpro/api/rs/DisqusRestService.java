package nl.vpro.api.rs;

import nl.vpro.api.transfer.DisqusThreadInfo;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Michiel Meeuwissen
 * @since 1.4
 */
@Path(DisqusRestService.PATH)
public interface DisqusRestService {
    public static final String PATH = "disqus";


    @GET
    @Path("details/{sitename}")
    @Produces(MediaType.APPLICATION_JSON+"; charset=UTF8")
    public Map<String, DisqusThreadInfo> getDetails(@PathParam("sitename") String siteName, @QueryParam("id") String... ids);


}
