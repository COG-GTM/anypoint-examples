package org.mule.examples.springboot.queryingadbandattachingresultstoanemail;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** {@code db:select} "Query Database" on the {@code MySQL_Configuration} generic config. */
@Repository
class EmployeeRepository {

    private final JdbcTemplate jdbc;

    EmployeeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<Map<String, Object>> findByName(String firstName, String lastName) {
        return jdbc.queryForList(
                "select first_name,last_name,gender,dob,hire_date from employees where first_name = ? and last_name = ?",
                firstName, lastName);
    }
}
