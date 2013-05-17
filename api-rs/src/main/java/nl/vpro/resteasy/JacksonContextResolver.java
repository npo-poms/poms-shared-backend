package nl.vpro.resteasy;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.map.ser.BeanPropertyFilter;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.FilterProvider;
import org.codehaus.jackson.map.ser.impl.SimpleFilterProvider;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.util.Collection;

/**
 * @author Michiel Meeuwissen
 * @since 2.0
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonContextResolver implements ContextResolver<ObjectMapper> {
    private ObjectMapper mapper;

    public JacksonContextResolver() throws Exception {
        this.mapper= new ObjectMapper();
        SerializationConfig config = mapper.getSerializationConfig();
        AnnotationIntrospector introspector = new AnnotationIntrospector.Pair(
                new JacksonAnnotationIntrospector(),
                new JaxbAnnotationIntrospector());


        mapper.setSerializationConfig(config
                .withAnnotationIntrospector(introspector)
                .withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL)
        );
//            .with(SerializationConfig.Feature.INDENT_OUTPUT)
//                .with(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS));

        mapper.setDeserializationConfig(mapper.getDeserializationConfig().withAnnotationIntrospector(introspector));

        FilterProvider filters = new SimpleFilterProvider().addFilter("CollectionsFilter",
                new BeanPropertyFilter() {

                    @Override
                    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov, BeanPropertyWriter writer) throws Exception {
                        Object value = writer.get(bean);
                        if (value instanceof Collection) {
                            Collection col = (Collection) value;
                            if (col.isEmpty()) {
                                return;
                            }
                        }
                        writer.serializeAsField(bean, jgen, prov);
                    }
                });
        mapper.setSerializationConfig(mapper.getSerializationConfig().withFilters(filters));

    }

    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        return mapper;
    }
}

