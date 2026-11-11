package org.mule.examples.springboot.choiceexceptionstrategy;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** choice-error-handlingFlow1: JSON to HashMap, validator filter, email regex filter, success message. */
@RestController
public class ChoiceErrorHandlingController {

    static final String PASSED = "Input data validation passed.";

    private final ObjectMapper objectMapper;
    private final InputDataValidator validator;
    private final EmailFilter emailFilter;
    private final MulePayloadRenderer renderer;

    public ChoiceErrorHandlingController(ObjectMapper objectMapper, InputDataValidator validator, EmailFilter emailFilter,
            MulePayloadRenderer renderer) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.emailFilter = emailFilter;
        this.renderer = renderer;
    }

    @PostMapping(path = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handle(@RequestBody String body) throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        Map<String, Object> order = objectMapper.readValue(body, LinkedHashMap.class);
        try {
            validator.validate(order);
        } catch (RuntimeException e) {
            throw new FlowException(renderer.render(order), e);
        }
        Object email = order.get("email");
        if (!emailFilter.accept(email)) {
            // A rejecting regex-filter stops the flow; the listener answers with an empty 200 response.
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(PASSED);
    }
}
