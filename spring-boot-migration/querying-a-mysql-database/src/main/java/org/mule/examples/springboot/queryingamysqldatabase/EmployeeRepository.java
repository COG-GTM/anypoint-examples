package org.mule.examples.springboot.queryingamysqldatabase;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends Repository<Employee, Integer> {

    /** The Mule {@code db:select}, with the query parameter bound instead of concatenated into the SQL. */
    @Query("select first_name from employees where last_name = :lastName")
    List<String> findFirstNamesByLastName(@Param("lastName") String lastName);
}
