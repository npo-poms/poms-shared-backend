package nl.vpro.domain.api.thesaurus;

import nl.vpro.domain.media.gtaa.GTAANewPerson;
import nl.vpro.domain.media.gtaa.GTAANewThesaurusObject;
import nl.vpro.domain.media.gtaa.GTAAPerson;
import nl.vpro.domain.media.gtaa.ThesaurusObject;

public interface JWTGTAAService {

    GTAAPerson submitPerson(GTAANewPerson newPerson, String jwt);

    ThesaurusObject submitThesaurusObject(GTAANewThesaurusObject gtaaNewThesaurusObject, String jwt);


}
