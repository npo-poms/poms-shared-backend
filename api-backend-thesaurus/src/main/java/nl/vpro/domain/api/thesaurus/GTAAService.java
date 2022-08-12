package nl.vpro.domain.api.thesaurus;

import org.checkerframework.checker.nullness.qual.NonNull;

import nl.vpro.domain.gtaa.*;


/**
 * The Service that facilitates submitting new GTAA concepts to the GTAA API from Beeld en Geluid
 */
public interface GTAAService {

    /**
     * Submits a new person to the GTAA.
     * <p>
     * This method is largely superseded by {@link #submitGTAAConcept(GTAANewConcept, String)}. A person is also a GTAA concept.
     *
     * @param newPerson The new person
     * @param jwt The JWT string for authentication. It will also be decrypted to get information about the 'creator'.
     */

    GTAAPerson submitGTAAPerson(@NonNull GTAANewPerson newPerson, @NonNull  String jwt);


    /**
     * Submits a new object to the GTAA.
     *
     * @param newObject The new person
     * @param jwt The JWT string for authentication. It will also be decrypted to get information about the 'creator'.
     */
    <T extends GTAAConcept, S extends GTAANewConcept>  T submitGTAAConcept(@NonNull S newObject, @NonNull String jwt);


}
