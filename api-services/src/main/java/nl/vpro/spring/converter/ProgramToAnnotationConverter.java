package nl.vpro.spring.converter;

import nl.vpro.api.domain.media.Program;
import nl.vpro.api.domain.media.Segment;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.domain.ugc.annotation.AnnotationPart;
import nl.vpro.domain.ugc.annotation.AnnotationPartTemplate;
import nl.vpro.domain.ugc.annotation.AnnotationType;
import org.pegdown.PegDownProcessor;
import org.springframework.core.convert.converter.Converter;

/**
 * Date: 17-4-12
 * Time: 12:55
 *
 * @author Ernst Bunders
 */
public class ProgramToAnnotationConverter implements Converter<Program, Annotation> {

    @Override
    public Annotation convert(Program program) {
        if (program.getSegments().size() == 0) {
            return null;
        }
        PegDownProcessor processor = new PegDownProcessor();

        Annotation annotation = new Annotation();
//        annotation.set
        annotation.setId(program.getUrn());
        annotation.setTitle("Standaard annotatie");
        annotation.setType(AnnotationType.TIMELINE);
        annotation.setMedia(program.getUrn());
        annotation.setCreationDate(program.getCreationDate());
        annotation.setLastModifiedDate(program.getLastModified());



        for (Segment segment : program.getSegments()) {
            AnnotationPart part = new AnnotationPart();
            part.setUrn(segment.getUrn());
            part.setStart(segment.getStart().getTime());
            part.setStop(segment.getStart().getTime() + segment.getDuration().getTime());
            if (segment.getImages().size() > 0) {
                part.setImage(segment.getImages().get(0).getImageUri());
            }
            part.setTemplate(AnnotationPartTemplate.TEXTIMAGELEFT);

            if (segment.getDescriptions().size() > 0) {
                part.setText(segment.getDescriptions().get(0).getValue());
                part.setHtml(processor.markdownToHtml(part.getText()));
            }
            annotation.addPart(part);
        }


        return annotation;

    }
}
