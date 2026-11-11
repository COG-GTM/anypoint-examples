package org.mule.examples.springboot.testingapikitwithmunit;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The four APIkit-generated flows for {@code /munit} in api.raml ({@code get:/munit:api-config}, ...). */
@RestController
@RequestMapping(path = "/api/munit", produces = MediaType.APPLICATION_JSON_VALUE)
public class MunitResourceController {

    @GetMapping
    public String get() {
        return "GET RESPONSE";
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String post() {
        return "POST RESPONSE";
    }

    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String put() {
        return "PUT RESPONSE";
    }

    @DeleteMapping
    public String delete() {
        return "DELETE RESPONSE";
    }
}
