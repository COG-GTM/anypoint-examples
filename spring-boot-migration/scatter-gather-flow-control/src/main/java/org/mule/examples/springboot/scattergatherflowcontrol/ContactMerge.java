package org.mule.examples.springboot.scattergatherflowcontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Straight port of {@code org.mule.examples.transformers.ContactMerge}. */
public final class ContactMerge {

    private ContactMerge() {
    }

    public static List<Map<String, String>> mergeList(List<Map<String, String>> contactsFromA,
                                                      List<Map<String, String>> contactsFromB) {
        List<Map<String, String>> merged = new ArrayList<>();
        for (Map<String, String> contactFromA : contactsFromA) {
            Map<String, String> mergedContact = createMergedContact(contactFromA);
            mergedContact.put("IDInA", contactFromA.get("Id"));
            merged.add(mergedContact);
        }
        for (Map<String, String> contact : contactsFromB) {
            Map<String, String> contactFromA = findContactInList(contact.get("Email"), merged);
            if (contactFromA != null) {
                contactFromA.put("IDInB", contact.get("Id"));
            } else {
                Map<String, String> mergedContact = createMergedContact(contact);
                mergedContact.put("IDInB", contact.get("Id"));
                merged.add(mergedContact);
            }
        }
        return merged;
    }

    private static Map<String, String> createMergedContact(Map<String, String> contact) {
        Map<String, String> mergedContact = new HashMap<>();
        mergedContact.put("Name", contact.get("Name"));
        mergedContact.put("Email", contact.get("Email"));
        mergedContact.put("IDInA", "");
        mergedContact.put("IDInB", "");
        return mergedContact;
    }

    private static Map<String, String> findContactInList(String email, List<Map<String, String>> contacts) {
        if (isBlank(email)) {
            return null;
        }
        for (Map<String, String> contact : contacts) {
            if (!isBlank(contact.get("Email")) && contact.get("Email").equals(email)) {
                return contact;
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
