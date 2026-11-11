package org.mule.examples.springboot.filteringamessage;

import java.util.Map;

/** Port of {@code org.mule.examples.filters.FreeMembershipDiscountFilter} (Mule {@code custom-filter}). */
public final class FreeMembershipDiscountFilter {

    private FreeMembershipDiscountFilter() {
    }

    public static boolean accept(Map<String, Object> payload) {
        boolean isPremium = payload.get("membership").equals("premium");
        int months = Integer.parseInt(payload.get("months").toString());
        int purchase = Integer.parseInt(payload.get("purchases").toString());
        if (isPremium) {
            return months >= 6 && purchase >= 1000;
        }
        return months <= 6 && purchase >= 2500
                || (months > 6 && months < 12 && purchase >= 2000)
                || (months >= 12 && purchase >= 1500);
    }
}
