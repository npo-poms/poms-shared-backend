package nl.vpro.domain.api.thesaurus;

import nl.vpro.domain.gtaa.*;

public interface GTAAService {

    /**
     * Submits a new person to the GTAA.
     *
     * @param newPerson The new person
     * @param jwt The JWT string for authentication. It will also be decrypted to get information about the 'creator'.
     */

    GTAAPerson submit(GTAANewPerson newPerson, String jwt);


    /**
     * Submits a new object to the GTAA.
     *
     * @param newObject The new person
     * @param jwt The JWT string for authentication. It will also be decrypted to get information about the 'creator'.
     */
    <T extends ThesaurusObject, S extends NewThesaurusObject>  T submit(S newObject, String jwt);


}
