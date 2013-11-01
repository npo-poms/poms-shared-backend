package nl.vpro.api.rs.client;

import nl.vpro.api.transfer.MediaSearchResultItem;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author Michiel Meeuwissen
 * @since 1.3
 */
public class ApiProfileTest {


    public static void main(String[] argv) throws IOException {
        ApiClient client = new ApiClient("http://localhost:8070/", 10000);
        Iterator<MediaSearchResultItem> items = client.getMediaRestService().getProfile("woord");
        while(items.hasNext()) {
            System.out.println(items.next());
        }
    }

}
