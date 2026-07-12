package com.parazit.panel.application.port.out.payment.receipt;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface ReceiptUploadSource {

    InputStream openStream() throws IOException;
}
