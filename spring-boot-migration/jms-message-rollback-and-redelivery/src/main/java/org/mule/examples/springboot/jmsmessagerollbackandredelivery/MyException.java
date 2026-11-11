package org.mule.examples.springboot.jmsmessagerollbackandredelivery;

/** Port of {@code org.exceptions.MyException}, thrown by the flow's test:component until the 5th delivery. */
public class MyException extends Exception {

    public MyException(String message) {
        super(message);
    }
}
