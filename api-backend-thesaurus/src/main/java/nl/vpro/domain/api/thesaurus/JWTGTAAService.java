package nl.vpro.domain.api.thesaurus;

import nl.vpro.domain.media.gtaa.GTAAPerson;

public interface JWTGTAAService {

    GTAAPerson submitPerson(String jwt);

}
