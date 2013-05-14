package nl.vpro.transfer.ugc.annotation;

import nl.vpro.domain.ugc.annotation.Annotation;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Date: 17-4-12
 * Time: 10:57
 *
 * @author Ernst Bunders
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@XmlRootElement(name = "annotationlist", namespace = "urn:vpro:ugc:2012")
@XmlAccessorType(XmlAccessType.FIELD)
public class Annotations {
    @XmlAttribute
    private Integer defaultAnnotationIndex = 0;
    @XmlElementWrapper(name = "annotations")
    @XmlElement(name = "annotation")
    private List<Annotation> annotations = new ArrayList<Annotation>();

    public Integer getDefaultAnnotationIndex() {
        return defaultAnnotationIndex;
    }

    public void setDefaultAnnotationIndex(Integer defaultAnnotationIndex) {
        this.defaultAnnotationIndex = defaultAnnotationIndex;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    public void addAnnotation(Annotation annotation) {
        this.annotations.add(annotation);
    }
}
