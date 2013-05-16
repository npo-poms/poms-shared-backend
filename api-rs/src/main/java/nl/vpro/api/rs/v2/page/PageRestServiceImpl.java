package nl.vpro.api.rs.v2.page;

import nl.vpro.domain.api.PagedResult;
import nl.vpro.domain.api.pages.Page;
import nl.vpro.domain.api.pages.PageBuilder;
import nl.vpro.domain.api.pages.PageForm;
import org.springframework.stereotype.Service;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Service
public class PageRestServiceImpl implements PageRestService {
	@Override
	public PagedResult<Page> list(String profile,  Integer offset, Integer limit, boolean mock) {
		return new PagedResult<>();
	}

	@Override
	public PagedResult<Page> search(PageForm form, String profile, Integer offset, @DefaultValue("50") Integer limit, @DefaultValue("false") boolean mock) {
		return new PagedResult<>();
	}

	@Override
	public Page get(@PathParam("id") String id, @DefaultValue("false") boolean mock) {
		if (mock) {
			return PageBuilder.id("4b748d32-8006-4f0a-8aac-6d8d5c89a847")
					.title("Groot brein in klein dier")
					.author("superuser")
					.summary("Een klein, harig beestje met het gewicht van een paperclip was mogelijk de directe voorouder van alle hedendaagse zoogdieren, waaronder de mens. Levend in de schaduw van de dinosaurussen kroop het diertje 195 miljoen jaar geleden tussen de planten door, op zoek naar insecten die het met zijn vlijmscherpe tandjes vermaalde. Het is de oudste zoogdierachtige die tot nu toe is gevonden.")
					.build();
		} else {
			throw new NotFoundException();
		}

	}
}
