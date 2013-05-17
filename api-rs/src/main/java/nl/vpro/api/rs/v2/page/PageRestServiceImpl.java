package nl.vpro.api.rs.v2.page;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.api.pages.Page;
import nl.vpro.domain.api.pages.PageBuilder;
import nl.vpro.domain.api.pages.PageType;
import nl.vpro.domain.api.pages.PageForm;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Service
public class PageRestServiceImpl implements PageRestService {
    private static int listSizes = 100;


    @Override
    public PagedResult<Page> list(String profile,  Integer offset, Integer limit, boolean mock) {
        if (mock) {
            return new PagedResult<>(mockList(listSizes, offset, limit), offset, listSizes);
        } else {
            return new PagedResult<>();
        }
    }

    @Override
    public PagedResult<Page> search(PageForm form, String profile, Integer offset, Integer limit, boolean mock) {
        if (mock) {
            return new PagedResult<>(mockList(listSizes, offset, limit), offset, listSizes);
        } else {
            return new PagedResult<>();
        }
    }

    @Override
    public Page get(String id, boolean mock) {
        if (mock) {
            return mockPage();
        } else {
            throw new NotFoundException();
        }

    }

    protected List<Page> mockList(int total, int offset, int limit) {
        int numberOfResults = Math.min(total - offset, limit);
        List<Page> result = new ArrayList<>();
        for (int i = 0; i < numberOfResults; i++) {
            result.add(mockPage());
        }
        return result;
    }


    protected Page mockPage()  {
        try {
            return PageBuilder.id("4b748d32-8006-4f0a-8aac-6d8d5c89a847")
                    .title("Groot brein in klein dier")
                    .author("superuser")
                    .summary("Een klein, harig beestje met het gewicht van een paperclip was mogelijk de directe voorouder van alle hedendaagse zoogdieren, waaronder de mens. Levend in de schaduw van de dinosaurussen kroop het diertje 195 miljoen jaar geleden tussen de planten door, op zoek naar insecten die het met zijn vlijmscherpe tandjes vermaalde. Het is de oudste zoogdierachtige die tot nu toe is gevonden.")
                    .body("bla bla bla bla")
                    .deepLink("http://www.wetenschap24.nl/groot-brein-in-klein-dier.html")
                    .pageType(PageType.Artikel)
                    .brand("http://www.wetenschap24.nl", "Wetenschap 24")
                    .mainImage("http://www.wetenschap24.nl/eenkleinharigbeest.jpg")
                    .build();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
