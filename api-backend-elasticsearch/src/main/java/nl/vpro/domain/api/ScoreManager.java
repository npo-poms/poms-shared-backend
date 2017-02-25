/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

/**
 * @author Roelof Jan Koekoek
 * @since 4.2
 */
public interface ScoreManager {

    String getTextFieldBoosts();

    void setTextField(String field, float boost);

}
