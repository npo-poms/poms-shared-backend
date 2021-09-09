package nl.vpro.semantic;

public interface VectorizationService {

    int VECTOR_LENGTH = nl.vpro.pages.domain.es.ApiPagesIndex.VECTOR_LENGTH;

    float[] forQuery(String query);

    float[] forText(String text);
}
