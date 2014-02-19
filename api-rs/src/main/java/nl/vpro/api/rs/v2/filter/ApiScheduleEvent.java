/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.filter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.vpro.domain.media.MediaObject;
import nl.vpro.domain.media.Program;
import nl.vpro.domain.media.ScheduleEvent;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@XmlType(name = "scheduleEventApiType", namespace = "urn:vpro:media:2009")
@XmlRootElement(name = "scheduleEvent", namespace = "urn:vpro:media:2009")
public class ApiScheduleEvent extends ScheduleEvent {

    private MediaObject mediaObject;


    @XmlElements({
        @XmlElement(name = "program", namespace = "urn:vpro:media:2009", type = Program.class)
    })

    @JsonProperty("media")
    public MediaObject getMedia() {
        return mediaObject;
    }

    @Override
    public void setMediaObject(MediaObject mediaObject) {
        this.mediaObject = mediaObject;
    }
}
