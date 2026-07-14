package com.parazit.panel.application.renewal;

import com.parazit.panel.application.xui.client.XuiClientRemoteOperationConnectionException;
import com.parazit.panel.application.xui.client.XuiClientRemoteOperationTimeoutException;
import com.parazit.panel.application.xui.client.XuiClientTrafficResetUnknownException;
import com.parazit.panel.application.xui.client.XuiClientUpdateUnknownException;
import com.parazit.panel.application.xui.client.XuiRemoteClientIdentityMismatchException;
import com.parazit.panel.application.xui.client.XuiRemoteClientMissingException;
import com.parazit.panel.domain.renewal.RenewalFailureClass;
import org.springframework.stereotype.Component;

@Component
public class RenewalFailureClassifier {

    public RenewalFailure classify(RuntimeException exception) {
        if (exception instanceof XuiClientRemoteOperationTimeoutException
                || exception instanceof XuiClientRemoteOperationConnectionException
                || exception instanceof XuiClientUpdateUnknownException
                || exception instanceof XuiClientTrafficResetUnknownException) {
            return new RenewalFailure(RenewalFailureClass.TRANSIENT, "REMOTE_TRANSIENT", true);
        }
        if (exception instanceof XuiRemoteClientMissingException) {
            return new RenewalFailure(RenewalFailureClass.MANUAL_REVIEW, "REMOTE_CLIENT_MISSING", false);
        }
        if (exception instanceof XuiRemoteClientIdentityMismatchException) {
            return new RenewalFailure(RenewalFailureClass.MANUAL_REVIEW, "REMOTE_IDENTITY_MISMATCH", false);
        }
        if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
            return new RenewalFailure(RenewalFailureClass.PERMANENT, "RENEWAL_VALIDATION_FAILED", false);
        }
        return new RenewalFailure(RenewalFailureClass.TRANSIENT, "RENEWAL_UNKNOWN_FAILURE", true);
    }
}
