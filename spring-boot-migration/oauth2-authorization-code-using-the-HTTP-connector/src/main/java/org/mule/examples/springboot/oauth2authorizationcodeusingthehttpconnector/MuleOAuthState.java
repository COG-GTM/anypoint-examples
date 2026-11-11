package org.mule.examples.springboot.oauth2authorizationcodeusingthehttpconnector;

/**
 * Mule 3.8 {@code StateEncoder}/{@code StateDecoder}: the resource owner id and the
 * onCompleteRedirectTo URL travel inside the OAuth {@code state} parameter as
 * {@code <original>:resourceOwnerId=<id>:onCompleteRedirectTo=<url>}.
 */
record MuleOAuthState(String originalState, String resourceOwnerId, String onCompleteRedirectTo) {

    static final String RESOURCE_OWNER = ":resourceOwnerId=";
    static final String ON_COMPLETE = ":onCompleteRedirectTo=";

    String encode() {
        String state = originalState;
        if (resourceOwnerId != null) {
            state = (state == null ? "" : state) + RESOURCE_OWNER + resourceOwnerId;
        }
        if (onCompleteRedirectTo != null) {
            state = (state == null ? "" : state) + ON_COMPLETE + onCompleteRedirectTo;
        }
        return state;
    }

    static MuleOAuthState decode(String state) {
        if (state == null) {
            return new MuleOAuthState(null, null, null);
        }
        int owner = state.indexOf(RESOURCE_OWNER);
        int complete = state.indexOf(ON_COMPLETE);
        int suffix = owner != -1 ? owner : complete;
        String original = suffix == -1 ? state : state.substring(0, suffix);
        String ownerId = null;
        if (owner != -1) {
            String rest = state.substring(owner + RESOURCE_OWNER.length());
            int end = rest.indexOf(ON_COMPLETE);
            ownerId = end == -1 ? rest : rest.substring(0, end);
        }
        String onComplete = complete == -1 ? null : state.substring(complete + ON_COMPLETE.length());
        return new MuleOAuthState(original.isEmpty() ? null : original, ownerId, onComplete);
    }
}
