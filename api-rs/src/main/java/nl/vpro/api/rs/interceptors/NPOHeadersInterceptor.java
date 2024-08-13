package nl.vpro.api.rs.interceptors;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import nl.vpro.VersionService;
import nl.vpro.domain.Roles;
import nl.vpro.domain.api.Result;
import nl.vpro.domain.api.media.Redirector;
import nl.vpro.poms.shared.ExtraHeaders;
import nl.vpro.poms.shared.Headers;


/**
 * @author Michiel Meeuwissen
 * @since 5.13
 */
@Provider
public class NPOHeadersInterceptor implements ContainerResponseFilter, ContainerRequestFilter {

    private static Predicate<String> aEqualsB(String a) {
        return b -> Objects.equals(a, b);
    }

    private static final List<Predicate<String>> RECOGNIZED = Roles.RECOGNIZED.stream().map(NPOHeadersInterceptor::aEqualsB).collect(Collectors.toCollection(ArrayList::new));


    public static void addRecognizedRolePredicate(Predicate<String>  role) {
        RECOGNIZED.add(role);
    }
    public static void addRecognizedRoles(String... roles) {
        Stream.of(roles).map(NPOHeadersInterceptor::aEqualsB).forEach(NPOHeadersInterceptor::addRecognizedRolePredicate);
    }


    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        try {
            Map<String, String> redirects = Redirector.REDIRECTS.get();
            if (redirects != null && !redirects.isEmpty()) {
                response.getHeaders().putSingle(Headers.NPO_REDIRECTS, redirects);
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                response.getHeaders().putSingle(Headers.NPO_CURRENT_USER, authentication.getName());
                if (authentication.getAuthorities() != null) {
                    response.getHeaders().putSingle(Headers.NPO_ROLES, authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(s -> RECOGNIZED.stream().anyMatch(p -> p.test(s)))
                        .map(a -> a.startsWith(Roles.ROLE) ? a.substring(Roles.ROLE.length()) : a)
                        .collect(Collectors.joining(",")));
                }
            }
            response.getHeaders().putSingle(Headers.NPO_VERSION, VersionService.version());

            ExtraHeaders.markUsed();
            ExtraHeaders.get().forEach(p -> {
                response.getHeaders().putSingle(p.getKey(), p.getValue());
            });
            Object entity = response.getEntity();
            if (entity instanceof Result) {
                response.getHeaders().putSingle(Headers.NPO_TOOK, ((Result<?>) entity).getTook());
            }
        } finally {
            ExtraHeaders.remove();
        }


    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Redirector.REDIRECTS.set(new HashMap<>());
    }
}
