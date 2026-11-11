package org.mule.examples.springboot.soapwebservicesecurity;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

/** Port of Mule's {@code com.mulesoft.mule.example.security.PasswordCallback} (joe/secret, stan/elephant). */
public class PasswordCallback implements CallbackHandler {

    @Override
    public void handle(Callback[] callbacks) {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback pc) {
                if ("joe".equals(pc.getIdentifier())) {
                    pc.setPassword("secret");
                } else if ("stan".equals(pc.getIdentifier())) {
                    pc.setPassword("elephant");
                }
            }
        }
    }
}
