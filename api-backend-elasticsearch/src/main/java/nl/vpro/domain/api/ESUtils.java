package nl.vpro.domain.api;

import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;

import org.elasticsearch.action.*;

public class ESUtils {

    public static <T> ActionListener<T> actionListener(CompletableFuture<T> future) {
        return new ActionListener<T>() {
            @Override
            public void onResponse(T t) {
                future.complete(t);
            }

            @Override
            public void onFailure(Exception e) {
                future.completeExceptionally(e);
            }
        };
    }

    public static String wildcard2regex(String wildcard) {
        StringBuilder regex = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(wildcard, "?*", true);
        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            switch (token) {
                case "?" -> regex.append(".");
                case "*" -> regex.append(".*");
                default ->
                    // Can't use Pattern.quote() as that uses \Q \E which is not supported by Lucene
                    regex.append(org.apache.lucene.queryparser.classic.QueryParser.escape(token));
            }
        }
        return regex.toString();
    }
}
