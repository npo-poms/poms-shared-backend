package nl.vpro.api.rs.v2.page;

import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.SearchResult;
import nl.vpro.domain.api.SearchResultItem;
import nl.vpro.domain.api.page.PageForm;
import nl.vpro.domain.api.page.PageService;
import nl.vpro.domain.page.Page;
import nl.vpro.domain.page.PageBuilder;
import nl.vpro.domain.page.PageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Service
public class PageRestServiceImpl implements PageRestService {
    private static long listSizes = 100l;


    private final PageService pageService;

    @Autowired
    PageRestServiceImpl(PageService pageService) {
        this.pageService = pageService;
    }


    @Override
    public Result<Page> list(String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return new Result<>(mockList(listSizes, offset, max), offset, max, listSizes);
        }
        return find(null, profile, offset, max, mock).asResult();
    }

    @Override
    public SearchResult<Page> find(PageForm form, String profile, Long offset, Integer max, boolean mock) {
        if(mock) {
            return new SearchResult<>(mockSearchItems(mockList(listSizes, offset, max)), offset, max, listSizes);
        }
        return pageService.find(form, profile, offset, max);
    }

    @Override
    public Page load(String id, boolean mock) {
        if(mock) {
            return mockPage();
        }
        return pageService.load(id);
    }

    protected List<Page> mockList(long total, long offset, int limit) {
        long numberOfResults = Math.min(total - offset, limit);
        List<Page> result = new ArrayList<>();
        for(int i = 0; i < numberOfResults; i++) {
            result.add(mockPage());
        }
        return result;
    }


    protected <T> List<SearchResultItem<T>> mockSearchItems(final List<T> list) {
        return new AbstractList<SearchResultItem<T>>() {

            @Override
            public SearchResultItem<T> get(int index) {
                SearchResultItem<T> result = new SearchResultItem<>(list.get(index));
                result.setScore(0.5f);
                //
                return result;
            }

            @Override
            public int size() {
                return list.size();
            }
        };
    }


    protected Page mockPage() {
        try {
            return PageBuilder.example().build();
        } catch(URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
