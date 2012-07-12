package nl.vpro.api.rs.util;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wat Apache kan, kunnen WIJ ook!
 * <p/>
 * Additional String conversion utilities
 */


public class StringUtil {

    /*
    * This Pattern will match on either quoted text or text between space, including
    * whitespace, and accounting for beginning and end of line.
    * Examples:
    * split("apple banana orange") = ["apple","banana","orange"]
    * split("apple    \"banana orange\" gorilla") = ["apple","banana orange","gorilla"]
    * split("apple \"banana orange") = ["apple", "\"banana", "orange"]
    *
    * This is useful when filtering a search query with strings that can contain spaces,
    * such as tags like "Europese Unie". So now you can search for << Subsidie "Europese Unie" >>
    */
    private static final Pattern spacesPattern = Pattern.compile("\"([^\"]*)\"|(?<= |^)([^ ]*)(?: |$)");

    /**
     * split("apple    \"banana orange\" gorilla") = ["apple","banana orange","gorilla"]
     *
     * @param spaceSeperatedString the string to split on spaces, unless surrounded with dbl quotes
     * @return
     */
    public static List<String> split(String spaceSeperatedString) {
        ArrayList<String> matches = new ArrayList<String>();
        Matcher matcher = spacesPattern.matcher(spaceSeperatedString);
        String match;
        while (matcher.find()) {
            match = matcher.group(1);
            if (match != null) {
                matches.add(match);
            } else {
                matches.add(matcher.group(2));
            }
        }
        List<String> results = new ArrayList<String>();
        for (int i = 0; i < matches.size(); i++) {
            if (!matches.get(i).equals("")) {
                results.add(matches.get(i).trim());
            }
        }
        //if you needed String[] instead, use the code below:
//        if (results.size() > 0) {
//            return results.toArray(new String[results.size()]);
//        } else {
//            return new String[0];
//        }
        return results;
    }

    /**
     * If the input string contains space(s), this method returns the input string between double quotes.
     * If the input contains no space(s), it returns the original input string.
     * Useful for Lucene Queries, see http://www.solrtutorial.com/solr-query-syntax.html for the lucent syntax.
     *
     * @param aString
     */
    public static String asLuceneQueryPhrase(final String aString) {
        String lqp = aString;
        if (StringUtils.containsAny(aString, " ")) {
            lqp = "\"" + aString + "\"";
        }
        return lqp;
    }
}

