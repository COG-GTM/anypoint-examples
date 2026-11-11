package org.mule.examples.springboot.queryingamysqldatabase;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** A row of the example's {@code employees} table (querying-a-mysql-database/src/main/resources/import.sql). */
@Table("employees")
public record Employee(@Id Integer no, LocalDate dob, String firstName, String lastName, String gender,
                       LocalDate hireDate) {
}
