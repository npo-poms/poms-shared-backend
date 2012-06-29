/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson;

import nl.vpro.api.domain.media.MediaObject;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ser.BeanPropertyFilter;
import org.codehaus.jackson.map.ser.BeanPropertyWriter;
import org.codehaus.jackson.map.ser.impl.SimpleFilterProvider;

import java.util.Collection;

/**
 * User: rico
 * Date: 03/04/2012
 */
public class MediaMapper extends ObjectMapper {

    private static BeanPropertyFilter collectionFilter =
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
        };


    public MediaMapper() {

        // Deserialization settings.
        DeserializationConfig deserializationConfig = copyDeserializationConfig();
        deserializationConfig.addHandler(new MediaProblemHandler());
        deserializationConfig.set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        setDeserializationConfig(deserializationConfig);
        //http://wiki.fasterxml.com/JacksonPolymorphicDeserialization#A1.1._Global_default_typing :
//        enableDefaultTyping(DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY);

        // Serialization settings.
        SerializationConfig serializationConfig = copySerializationConfig();
        serializationConfig.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        serializationConfig.set(SerializationConfig.Feature.INDENT_OUTPUT, true);
        serializationConfig.set(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, true);
        SimpleFilterProvider filter = new SimpleFilterProvider();
        filter.setDefaultFilter(collectionFilter);
        filter.addFilter(MediaObject.class.getSimpleName(), collectionFilter);

        setSerializationConfig(serializationConfig.withFilters(filter));
    }


}
