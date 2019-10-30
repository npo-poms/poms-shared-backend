package nl.vpro.domain.api;

import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;

import org.elasticsearch.action.*;

public class ESUtils {

    public static <T> CompletableFuture<T> fromListenableActionFuture(ListenableActionFuture<T> future) {
        CompletableFuture<T> cFuture = new CompletableFuture<>();
        future.addListener(new ActionListener<T>() {
            @Override
            public void onResponse(T t) {
                cFuture.complete(t);
            }

            @Override
            public void onFailure(Exception e) {
                cFuture.completeExceptionally(e);
            }
        });
        return cFuture;
    }

    public static <T> CompletableFuture<T> fromActionFuture(ActionFuture<T> future) {

        CompletableFuture<T> cFuture = CompletableFuture.<T>supplyAsync(future::actionGet);
        return cFuture;

    }

    public static String wildcard2regex(String wildcard) {
        StringBuilder regex = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(wildcard, "?*", true);
        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            switch (token) {
                case "?":
                    regex.append(".");
                    break;
                case "*":
                    regex.append(".*");
                    break;
                default:
                    // Can't use Pattern.quote() as that uses \Q \E which is not supported by Lucene
                    regex.append(org.apache.lucene.queryparser.classic.QueryParser.escape(token));
                    break;
            }
        }
        return regex.toString();
    }
}
