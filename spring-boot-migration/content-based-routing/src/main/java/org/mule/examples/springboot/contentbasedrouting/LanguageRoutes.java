package org.mule.examples.springboot.contentbasedrouting;

/**
 * Routing table of the Mule {@code choice} in {@code content-based-routingFlow}: {@code flowVars['language'] == 'Spanish'},
 * {@code == 'French'}, otherwise the {@code replyInDefaultLanguage} sub-flow.
 */
public final class LanguageRoutes {

    public static final String SPANISH = "replyInSpanish";
    public static final String FRENCH = "replyInFrench";
    public static final String DEFAULT = "replyInDefaultLanguage";

    private LanguageRoutes() {
    }

    public static String destination(String language) {
        if ("Spanish".equals(language)) {
            return SPANISH;
        }
        if ("French".equals(language)) {
            return FRENCH;
        }
        return DEFAULT;
    }
}
