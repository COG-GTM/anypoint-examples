package org.mule.examples.springboot.scattergatherflowcontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The two {@code expression-component} routes of the scatter-gather (static contact lists). */
public final class ContactSources {

    private ContactSources() {
    }

    public static List<Map<String, String>> sourceA() {
        return List.of(contact("1", "vlado", "vlado@email.com"), contact("2", "michal", "michal@email.com"));
    }

    public static List<Map<String, String>> sourceB() {
        return List.of(contact("1", "peter", "peter@email.com"), contact("2", "vlado", "vlado@email.com"));
    }

    private static Map<String, String> contact(String id, String name, String email) {
        Map<String, String> contact = new HashMap<>();
        contact.put("Id", id);
        contact.put("Name", name);
        contact.put("Email", email);
        return contact;
    }
}
