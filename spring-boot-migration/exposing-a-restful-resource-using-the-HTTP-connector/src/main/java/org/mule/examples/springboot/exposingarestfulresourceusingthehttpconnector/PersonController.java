package org.mule.examples.springboot.exposingarestfulresourceusingthehttpconnector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PersonController {

    static final String CREATED_BODY =
            "{ \"status\": \"success\", \"statusDescription\": \"person created successfully\"}";
    static final String FILTER_REJECTED =
            "Message has been rejected by filter. Message payload is of type: NullPayload";
    static final String LOCATION_PREFIX = "http://0.0.0.0:8081/person/";

    private static final Logger log = LoggerFactory.getLogger(PersonController.class);

    private final PersonStore store;
    private final ObjectMapper mapper;

    public PersonController(PersonStore store, ObjectMapper mapper) {
        this.store = store;
        this.mapper = mapper;
    }

    /** Mule flow {@code retrievePersonsFlow}. */
    @GetMapping(path = "/person", produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<Person> all() {
        return store.all();
    }

    /**
     * Mule flow {@code storePersonFlow}; the Location header is hard-coded to 0.0.0.0:8081 exactly as in the Mule
     * response-builder. Like {@code json:json-to-object-transformer}, the body is parsed as JSON whatever its
     * Content-Type.
     */
    @PostMapping(path = "/person")
    public ResponseEntity<String> create(@RequestBody String body) throws JsonProcessingException {
        int id = store.add(mapper.readValue(body, Person.class));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", LOCATION_PREFIX + id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CREATED_BODY);
    }

    /** Mule flow {@code retrievePersonFlow}. */
    @GetMapping(path = "/person/{personId}")
    public ResponseEntity<?> one(@PathVariable String personId) {
        int id;
        try {
            id = Integer.valueOf(personId);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        log.error("Person id is {}", id);
        return store.find(id)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(p))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_PLAIN).body(FILTER_REJECTED));
    }

    /** Unparseable JSON fails json-to-object-transformer; storePersonFlow has no exception strategy, so Mule answers 500. */
    @ExceptionHandler(JsonProcessingException.class)
    ResponseEntity<Void> invalidJson() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
