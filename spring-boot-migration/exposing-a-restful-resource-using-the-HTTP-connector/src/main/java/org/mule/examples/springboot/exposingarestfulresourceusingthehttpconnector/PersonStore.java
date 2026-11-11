package org.mule.examples.springboot.exposingarestfulresourceusingthehttpconnector;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Replaces the Mule spring beans {@code personDataStore} (HashMap) and {@code personId} (AtomicInteger). */
@Component
public class PersonStore {

    private final Map<Integer, Person> people = new ConcurrentSkipListMap<>();
    private final AtomicInteger personId = new AtomicInteger();

    public int add(Person person) {
        int id = personId.incrementAndGet();
        people.put(id, person);
        return id;
    }

    public Optional<Person> find(int id) {
        return Optional.ofNullable(people.get(id));
    }

    public Collection<Person> all() {
        return people.values();
    }
}
