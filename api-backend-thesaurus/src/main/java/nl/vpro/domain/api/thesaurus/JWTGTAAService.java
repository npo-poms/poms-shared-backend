package nl.vpro.domain.api.thesaurus;

import nl.vpro.domain.media.gtaa.*;

public interface JWTGTAAService {

    GTAAPerson submit(GTAANewPerson newPerson, String jwt);

    <T extends ThesaurusObject, S extends NewThesaurusObject<T>>  T submit(S gtaaNewThesaurusObject, String jwt);


}
