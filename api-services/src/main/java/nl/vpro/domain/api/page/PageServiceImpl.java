package nl.vpro.domain.api.page;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Named;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import nl.vpro.domain.Roles;
import nl.vpro.domain.api.IdList;
import nl.vpro.domain.api.SuggestResult;
import nl.vpro.domain.api.profile.*;
import nl.vpro.domain.api.profile.exception.ProfileNotFoundException;
import nl.vpro.domain.api.suggest.QuerySearchRepository;
import nl.vpro.domain.page.Page;
import nl.vpro.util.CloseableIterator;
import nl.vpro.util.FilteringIterator;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Service
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
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("PageService.suggest")
    public SuggestResult suggest(String input, String profile, Integer max) throws ProfileNotFoundException {
        return querySearchRepository.suggest(input, getProfile(profile) != null ? profile : null, max);
    }

    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("PageService.find")
    public PageSearchResult find(PageForm form, String profile, Long offset, Integer max) throws ProfileNotFoundException {
        return pageSearchRepository.find(getProfile(profile), form, offset, max);
    }

    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("PageService.load")
    public Page load(String url) {
        return pageSearchRepository.load(url);
    }

    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("PageService.loadByCrid")
    public Page[] loadByCrid(String... crid) {
        return pageSearchRepository.loadByCrid(crid);
    }

    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("PageService.loadForIds")
    public List<Page> loadForIds(IdList ids) {
        String[] idArray = ids.toArray(new String[0]);
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
                }

            }
            // ready, so we don't need the rest.
            urlPages.cancel(true);
            cridPages.cancel(true);
            statRefPages.cancel(true);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    @PreAuthorize(Roles.HAS_API_ROLE)
    @Cacheable("PageService.findRelated")
    public PageSearchResult findRelated(Page page, String profile, PageForm form, Integer max) throws ProfileNotFoundException {
        return pageSearchRepository.findRelated(page, getProfile(profile), form, max);
    }


    @Override
    @PreAuthorize(Roles.HAS_API_CHANGES_ROLE)
    public CloseableIterator<Page> iterate(String profile, PageForm form, Long offset, Integer max, FilteringIterator.KeepAlive keepAlive) throws ProfileNotFoundException {
        return pageSearchRepository.iterate(getProfile(profile), form, offset, max, keepAlive);

    }


    private ProfileDefinition<Page> getProfile(String profile) throws ProfileNotFoundException {
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
