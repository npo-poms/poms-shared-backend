package nl.vpro.domain.api.page;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Named;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import nl.vpro.domain.api.IdList;
import nl.vpro.domain.api.SuggestResult;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.api.profile.ProfileDefinition;
import nl.vpro.domain.api.profile.ProfileService;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.domain.api.suggest.QuerySearchRepository;
import nl.vpro.domain.page.Page;
import nl.vpro.util.FilteringIterator;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Service
@PreAuthorize("hasRole('ROLE_EVERYBODY_EXCLUDED')")
public class PageServiceImpl implements PageService {
    private final ProfileService profileService;

    private final PageSearchRepository pageSearchRepository;

    private final QuerySearchRepository querySearchRepository;

    @Autowired
    public PageServiceImpl(
        ProfileService profileService,
        PageSearchRepository pageSearchRepository,
        @Named("pageQueryRepository") QuerySearchRepository querySearchRepository) {
        this.profileService = profileService;
        this.pageSearchRepository = pageSearchRepository;
        this.querySearchRepository = querySearchRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_API_CLIENT', 'ROLE_API_USER', 'ROLE_API_SUPERUSER', 'ROLE_API_SUPERCLIENT', 'ROLE_API_SUPERPROCESS')")
    public SuggestResult suggest(String input, String profile, Integer max) {
        return querySearchRepository.suggest(input, getProfile(profile) != null ? profile : null, max);
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_API_CLIENT', 'ROLE_API_USER', 'ROLE_API_SUPERUSER', 'ROLE_API_SUPERCLIENT', 'ROLE_API_SUPERPROCESS')")
    public PageSearchResult find(PageForm form, String profile, Long offset, Integer max) {
        return pageSearchRepository.find(getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_API_CLIENT', 'ROLE_API_USER', 'ROLE_API_SUPERUSER', 'ROLE_API_SUPERCLIENT', 'ROLE_API_SUPERPROCESS')")
    public Page load(String url) {
        return pageSearchRepository.load(url);
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_API_CLIENT', 'ROLE_API_USER', 'ROLE_API_SUPERUSER', 'ROLE_API_SUPERCLIENT', 'ROLE_API_SUPERPROCESS')")
    public Page[] loadByCrid(String... crid) {
        return pageSearchRepository.loadByCrid(crid);
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_API_CLIENT', 'ROLE_API_USER', 'ROLE_API_SUPERUSER', 'ROLE_API_SUPERCLIENT', 'ROLE_API_SUPERPROCESS')")
    public List<Page> loadForIds(IdList ids) {
        String[] idArray = ids.toArray(new String[ids.size()]);
        CompletableFuture<Page[]> urlPages     = pageSearchRepository.loadByUrlsAsync(idArray);
        CompletableFuture<Page[]> cridPages    = pageSearchRepository.loadByCridsAsync(idArray);
        CompletableFuture<Page[]> statRefPages = pageSearchRepository.loadByStatRefsAsync(idArray);


        try {
            List<Page> result = new ArrayList<>(ids.size());
            for (int i = 0; i < ids.size(); i++) {
                result.add(null);
                Page urlPage = urlPages.get()[i];
                if (urlPage != null) {
                    result.set(i, urlPage);
                    continue;
                }
                Page cridPage = cridPages.get()[i];
                if (cridPage != null) {
                    result.set(i, cridPage);
                    continue;
                }
                Page statRefPage = statRefPages.get()[i];
                if (statRefPage != null) {
                    result.set(i, statRefPage);
                    continue;
                }

            }
            // ready, so we don't need the rest.
            urlPages.cancel(true);
            cridPages.cancel(true);
            statRefPages.cancel(true);
            return result;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    @PreAuthorize("hasAnyRole('ROLE_API_CLIENT', 'ROLE_API_USER', 'ROLE_API_SUPERUSER', 'ROLE_API_SUPERCLIENT', 'ROLE_API_SUPERPROCESS')")
    public PageSearchResult findRelated(Page page, String profile, PageForm form, Integer max) throws ProfileNotFoundException {
        return pageSearchRepository.findRelated(page, getProfile(profile), form, max);
    }


    @Override
    @PreAuthorize("hasAnyRole('ROLE_API_CLIENT', 'ROLE_API_SUPERCLIENT', 'ROLE_API_USER', 'ROLE_API_SUPERUSER')")
    public Iterator<Page> iterate(String profile, PageForm form, Long offset, Integer max, FilteringIterator.KeepAlive keepAlive) throws ProfileNotFoundException {
        return pageSearchRepository.iterate(getProfile(profile), form, offset, max, keepAlive);

    }


    private ProfileDefinition<Page> getProfile(String profile) {
        if(profile == null || "".equals(profile) // handy for scripting (profile=$2 and so on...)
        ) {
            return null;
        }
        Profile p = profileService.getProfile(profile);
        if(p == null) {
            throw new ProfileNotFoundException(profile);
        }
        return p.getPageProfile();

    }
}
