package nl.vpro.api.rs.filter;

import java.util.Set;
import java.util.SortedSet;

import nl.vpro.domain.media.support.TextualType;
import nl.vpro.domain.media.support.Title;
import nl.vpro.domain.media.support.Typable;
import nl.vpro.util.ResortedSortedSet;

/**
 * @author Michiel Meeuwissen
 * @since 5.0
 */
public class FilteredSortedTitleSet extends FilteredSortedTextualTypableSet<Title> {


    private FilteredSortedTitleSet(String property, SortedSet<Title> wrapped) {
        super(property, wrapped);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Typable<TextualType>> FilteredSortedTitleSet wrapTitles(String property, Set<T> wrapped) {
        if (!(wrapped instanceof SortedSet)) {
            wrapped = new ResortedSortedSet<>(wrapped);
        }

        if (wrapped instanceof FilteredSortedTitleSet) {
            if (!(((FilteredSortedTitleSet) wrapped).filterHelper.property).equals(property)) {
                throw new IllegalArgumentException("Can't wrap different properties");
            }

            return (FilteredSortedTitleSet) wrapped;
        }

        return new FilteredSortedTitleSet(property, (SortedSet<Title>) wrapped);
    }

}
