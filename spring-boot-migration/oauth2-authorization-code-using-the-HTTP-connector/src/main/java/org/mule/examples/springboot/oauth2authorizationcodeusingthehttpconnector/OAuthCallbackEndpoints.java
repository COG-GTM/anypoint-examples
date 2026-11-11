package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

/**
 * The two listeners Mule's authorization-code grant type opens by itself, on the paths of
 * {@code localAuthorizationUrl} and {@code redirectionUrl} (any HTTP method, as Mule's dynamic flows).
 */
@Configuration
class OAuthCallbackEndpoints {

    static String path(String url) {
        String path = URI.create(url).getPath();
        return path == null || path.isEmpty() ? "/" : path;
    }

    @Bean
    RouterFunction<ServerResponse> oauthCallbackRoutes(BoxAuthorizationCodeClient oauth,
                                                       @Value("${oauth.local-authorization-url}") String localAuthorizationUrl,
                                                       @Value("${oauth.redirection-url}") String redirectionUrl) {
        return RouterFunctions.route()
                .route(RequestPredicates.path(path(localAuthorizationUrl)), request -> ServerResponse.status(HttpStatus.FOUND)
                        .header("Location", oauth.authorizationRequestLocation(request.param("onCompleteRedirectTo").orElse(null)))
                        .build())
                .route(RequestPredicates.path(path(redirectionUrl)), request -> {
                    HttpServletRequest servlet = request.servletRequest();
                    String uri = servlet.getRequestURI() + (servlet.getQueryString() == null ? "" : "?" + servlet.getQueryString());
                    BoxAuthorizationCodeClient.CallbackResult result = oauth.handleRedirect(
                            request.param("code").orElse(null), request.param("state").orElse(null), uri);
                    ServerResponse.BodyBuilder response = ServerResponse.status(result.httpStatus()).contentType(MediaType.TEXT_PLAIN);
                    if (result.location() != null) {
                        response.header("Location", result.location());
                    }
                    return response.body(result.body());
                })
                .build();
    }
}
