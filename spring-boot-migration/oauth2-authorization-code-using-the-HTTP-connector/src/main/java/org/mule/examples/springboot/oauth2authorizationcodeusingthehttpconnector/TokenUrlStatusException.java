package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

import org.springframework.web.client.RestClientException;

/** Token URL answered >= 400 (Mule AbstractTokenRequestHandler#invokeTokenUrl → TokenUrlResponseException). */
class TokenUrlStatusException extends RestClientException {

    TokenUrlStatusException(int status) {
        super("Token URL returned status " + status);
    }

    static boolean isCause(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof TokenUrlStatusException) {
                return true;
            }
        }
        return false;
    }
}
