package nl.vpro.semantic;

public interface VectorizationService {

    float[] forQuery(String query);

    float[] forText(String text);
}
