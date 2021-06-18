package nl.vpro.semantic;

public interface VectorizationService {

    int VECTOR_LENGTH = 512;

    float[] forQuery(String query);

    float[] forText(String text);
}
