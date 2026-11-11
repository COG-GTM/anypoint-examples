-- Schema from src/main/resources/import_orders.sql and the rows the Mule IT seeds (MySQLDbCreator#setUpDatabase).
DROP TABLE IF EXISTS employees;
CREATE TABLE employees (
no INT NOT NULL,
dob DATE NOT NULL,
first_name VARCHAR(14) NOT NULL,
last_name VARCHAR(16) NOT NULL,
gender ENUM ('M','F') NOT NULL,
hire_date DATE NOT NULL,
PRIMARY KEY (no)
);
INSERT INTO employees (no,dob,first_name,last_name,gender,hire_date) VALUES (1011,'1985-09-02','Chava','Puckett','F','2008-10-12');
INSERT INTO employees (no,dob,first_name,last_name,gender,hire_date) VALUES (1066,'1971-10-21','Quentin','Puckett','F','08-09-15');
INSERT INTO employees (no,dob,first_name,last_name,gender,hire_date) VALUES (1067,'1950-09-26','Mona','Sosa','M','07-11-27');
