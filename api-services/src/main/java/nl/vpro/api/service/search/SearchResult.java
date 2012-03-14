package nl.vpro.api.service.search;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Date: 13-3-12
 * Time: 18:05
 *
 * @author Ernst Bunders
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class SearchResult {
    @XmlAttribute
    private long numFound = 0;
    
    @XmlAttribute
    private long start = 0;
    
    @XmlAttribute
    private Float maxScore = null;
    
    @XmlElementWrapper(name = "documents")
    @XmlElement(name = "document")
    private List<Map<String, Object>> documents = new ArrayList<Map<String, Object>>();

    public SearchResult() {
    }

    public SearchResult(long numFound, long start, Float maxScore) {
        this.numFound = numFound;
        this.start = start;
        this.maxScore = maxScore;
    }

    public Float getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Float maxScore) {
        this.maxScore = maxScore;
    }

    public long getNumFound() {
        return numFound;
    }

    public void setNumFound(long numFound) {
        this.numFound = numFound;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public List<Map<String, Object>> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Map<String, Object>> documents) {
        this.documents = documents;
    }

    public void addDocument(Map<String, Object> document) {
        documents.add(document);
    }

    @Override
    public String toString() {
        return "{numFound=" + numFound
            + ",start=" + start
            + (maxScore != null ? "" + maxScore : "")
            + ",docs=" + documents.toString()
            + "}";
    }
}
