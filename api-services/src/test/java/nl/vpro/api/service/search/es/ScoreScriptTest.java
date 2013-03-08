/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.service.search.es;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * User: rico
 * Date: 08/03/2013
 */
public class ScoreScriptTest {

    @Test
    public void GenerateScriptTest() {
        Map<String,Float> scoreTable = new HashMap<String, Float>();
        String expectedValue = "scoreTable = ['Artikel' : 2.00, 'Audio' : 1.30, 'Video' : 1.50]; value = doc['pagetype'].value; _score * (scoreTable contains value ? scoreTable[value] : 1.0)";

        scoreTable.put("Video",1.5f);
        scoreTable.put("Audio",1.3f);
        scoreTable.put("Artikel",2.0f);

        String script = ScoreScriptGenerator.generate("pagetype", scoreTable);
        assertEquals(expectedValue, script);
    }
}
