package org.mule.examples.springboot.loginformusingthehttpconnector;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

/** Replaces the Mule {@code parse-template} elements; only {@code #[payload.username]} is used by the templates. */
@Component
public class LoginTemplates {

    private static final String USERNAME_EXPRESSION = "#[payload.username]";

    public String page() {
        return load("login/index.html");
    }

    public String success(String username) {
        return render("login/loginSuccessful.html", username);
    }

    public String failure(String username) {
        return render("login/loginFailure.html", username);
    }

    private String render(String location, String username) {
        return load(location).replace(USERNAME_EXPRESSION, HtmlUtils.htmlEscape(String.valueOf(username)));
    }

    private static String load(String location) {
        try {
            return StreamUtils.copyToString(new ClassPathResource(location).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
