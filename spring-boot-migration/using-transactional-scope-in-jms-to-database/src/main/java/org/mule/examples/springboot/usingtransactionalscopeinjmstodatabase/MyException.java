package org.mule.examples.springboot.usingtransactionalscopeinjmstodatabase;

/** org.exceptions.MyException, thrown by the TestComponent after the insert. */
public class MyException extends Exception {

    public MyException(String message) {
        super(message);
    }
}
