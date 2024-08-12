package nl.vpro.api.rs.filter;

import java.util.Set;
import java.util.SortedSet;

import nl.vpro.domain.media.support.Description;
import nl.vpro.util.ResortedSortedSet;

/**
 * @author Michiel Meeuwissen
 * @since 5.0
 */
public class FilteredSortedDescriptionSet extends FilteredSortedTextualTypableSet<Description> {


    private FilteredSortedDescriptionSet(String property, SortedSet<Description> wrapped) {
        super(property, wrapped);
    }

    public static FilteredSortedDescriptionSet wrapDescriptions(String property, Set<Description> wrapped) {
        if (!(wrapped instanceof SortedSet)) {
            wrapped = ResortedSortedSet.of(wrapped);
        }

        if (wrapped instanceof FilteredSortedTextualTypableSet) {
            if (!(((FilteredSortedTextualTypableSet) wrapped).filterHelper.property).equals(property)) {
                throw new IllegalArgumentException("Can't wrap different properties");
            }

            return (FilteredSortedDescriptionSet) wrapped;
        }

        return new FilteredSortedDescriptionSet(property, (SortedSet<Description>) wrapped);
    }

}
