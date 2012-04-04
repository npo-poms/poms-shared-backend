/*
 * Copyright (C) 2012 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.jackson;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * User: rico
 * Date: 03/04/2012
 */
public class MediaMapper extends ObjectMapper {


    public MediaMapper() {

        // Deserialization settings.
        DeserializationConfig deserializationConfig = copyDeserializationConfig();
        deserializationConfig.addHandler(new MediaProblemHandler());
        deserializationConfig.set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        setDeserializationConfig(deserializationConfig);

        // Serialization settings.
        SerializationConfig serializationConfig = copySerializationConfig();
        serializationConfig.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        serializationConfig.set(SerializationConfig.Feature.INDENT_OUTPUT, true);
        serializationConfig.set(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, true);
        setSerializationConfig(serializationConfig);
    }
}
