/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service.search.es;

import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

/**
 * User: rico
 * Date: 08/03/2013
 */
public class ScoreScriptGenerator {

    public static String generate(String field, Map<String, Float> scoreTable) {
        StringBuffer script = new StringBuffer();

        script.append("scoreTable = [");
        boolean first = true;
        for (Map.Entry<String, Float> entry : scoreTable.entrySet()) {
            script.append(new Formatter(Locale.US).format("%s'%s' : %.2f", first ? "" : ", ", entry.getKey(), entry.getValue()));
            first=false;
        }
        script.append("]; ");
        script.append("value = doc['").append(field).append("'].value; ");
        script.append("_score * (scoreTable contains value ? scoreTable[value] : 1.0)");

        return script.toString();
    }
}
