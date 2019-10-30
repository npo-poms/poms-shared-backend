package nl.vpro.domain.api.page;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import nl.vpro.domain.api.IdList;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.api.suggest.QuerySearchRepository;
import nl.vpro.domain.page.Page;
import nl.vpro.domain.page.PageBuilder;
import nl.vpro.domain.page.PageType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
public class PageServiceImplTest {

    final ProfileService profileService = mock(ProfileService.class);

    final PageSearchRepository pageSearchRepository = mock(PageSearchRepository.class);

    final QuerySearchRepository querySearchRepository = mock(QuerySearchRepository.class);

    final PageServiceImpl impl = new PageServiceImpl(profileService, pageSearchRepository, querySearchRepository);

    @Test
    public void testFind() throws Exception {
        when(profileService.getProfile("bla")).thenReturn(mock(Profile.class));
        impl.find(null, "bla", 0L, 10);
        verify(profileService).getProfile("bla");
        verify(pageSearchRepository).find(null, null, 0L, 10);
    }

    @Test
    public void testLoad() throws Exception {
        impl.load("1234");
        verify(pageSearchRepository).load("1234");
    }

    @Test
    public void testLoadForIds() {
        String url = "http://a";
        String alternativeUrl = "http://b";
        String crid = "crid://c";
        String statRef1 = "d";
        String statRef2 = "e";
        Page urlPage = PageBuilder.page(PageType.ARTICLE).url(url).build();
        Page alternativeUrlPage = PageBuilder.page(PageType.ARTICLE).alternativeUrls(alternativeUrl).build();
        Page cridPage = PageBuilder.page(PageType.ARTICLE).crids(crid).build();
        Page statRef2Page = PageBuilder.page(PageType.ARTICLE).statRefs(statRef2).build();

        String[] args = new String[]{url, alternativeUrl, crid, statRef1, statRef2};
        IdList ids = new IdList(args);


        when(pageSearchRepository.loadByUrlsAsync(args)).thenReturn(
            CompletableFuture.completedFuture(new Page[] {urlPage, alternativeUrlPage, null, null, null}));
        when(pageSearchRepository.loadByCridsAsync(args)).thenReturn(
            CompletableFuture.completedFuture(new Page[] { null, null, cridPage, null, null}));
        when(pageSearchRepository.loadByStatRefsAsync(args)).thenReturn(
            CompletableFuture.completedFuture(new Page[]{null, null, null, null, statRef2Page}));

        assertThat(impl.loadForIds(ids)).containsExactly(urlPage, alternativeUrlPage, cridPage, null, statRef2Page);
    }


}
