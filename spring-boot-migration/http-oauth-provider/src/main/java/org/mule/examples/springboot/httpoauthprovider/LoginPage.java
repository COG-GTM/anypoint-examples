package org.mule.examples.springboot.httpoauthprovider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;

/** The provider's login form, shown by GET /authorize while the resource owner is not logged in. */
final class LoginPage {

    private LoginPage() {
    }

    static void render(HttpServletRequest request, HttpServletResponse response, String providerName, int status, String error)
            throws IOException {
        String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
        String action = HtmlUtils.htmlEscape(request.getRequestURI() + query);
        String name = HtmlUtils.htmlEscape(providerName);
        response.setStatus(status);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("<!DOCTYPE html>\n<html><head><title>" + name + "</title></head><body>\n"
                + "<h1>" + name + "</h1>\n"
                + (error == null ? "" : "<p class=\"error\">" + HtmlUtils.htmlEscape(error) + "</p>\n")
                + "<form method=\"post\" action=\"" + action + "\">\n"
                + "<label for=\"username\">Username</label> <input type=\"text\" id=\"username\" name=\"username\"/>\n"
                + "<label for=\"password\">Password</label> <input type=\"password\" id=\"password\" name=\"password\"/>\n"
                + "<button type=\"submit\">Login and Authorize</button>\n"
                + "</form>\n</body></html>\n");
    }
}
