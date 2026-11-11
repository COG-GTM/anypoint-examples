package org.mule.examples.springboot.queryingamysqldatabase;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** database-to-jsonFlow: HTTP listener on {@code /}, {@code db:select}, {@code json:object-to-json-transformer}. */
@RestController
public class DatabaseToJsonController {

    private final EmployeeRepository employees;

    public DatabaseToJsonController(EmployeeRepository employees) {
        this.employees = employees;
    }

    @RequestMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> query(@RequestParam(name = "lastname", required = false) String lastname) {
        // Mule's MEL template renders a missing parameter as the text "null".
        String lastName = lastname == null ? "null" : lastname;
        return employees.findFirstNamesByLastName(lastName).stream()
                .map(firstName -> Map.<String, Object>of("first_name", firstName))
                .toList();
    }
}
