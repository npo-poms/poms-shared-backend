package nl.vpro.domain.api.thesaurus;

import nl.vpro.domain.media.gtaa.GTAAPerson;
import nl.vpro.rs.thesaurus.update.NewPerson;

public interface JWTGTAAService {

    GTAAPerson submitPerson(NewPerson newPerson, String jwt);


}
