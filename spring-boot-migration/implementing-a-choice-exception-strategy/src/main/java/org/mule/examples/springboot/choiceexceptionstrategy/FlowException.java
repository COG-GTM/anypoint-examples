package org.mule.examples.springboot.choiceexceptionstrategy;

/** Carries the message payload at the time of failure, like a Mule MessagingException, to the exception strategy. */
public class FlowException extends RuntimeException {

    private final transient Object payload;

    public FlowException(Object payload, Throwable cause) {
        super(cause.getMessage(), cause);
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

    public boolean causedBy(Class<? extends Throwable> type) {
        for (Throwable t = getCause(); t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}
