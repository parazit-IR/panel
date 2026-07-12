package com.parazit.panel.application.payment.manual.receipt;

import com.parazit.panel.application.payment.manual.receipt.query.GetManualPaymentReceiptContentQuery;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReceiptContentResult;
import com.parazit.panel.application.port.in.payment.manual.receipt.GetManualPaymentReceiptContentUseCase;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptContent;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptStorage;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetManualPaymentReceiptContentService implements GetManualPaymentReceiptContentUseCase {

    private final ManualPaymentReceiptRepository receiptRepository;
    private final PaymentReceiptStorage storage;
    private final ManualPaymentReceiptResultMapper mapper;

    public GetManualPaymentReceiptContentService(
            ManualPaymentReceiptRepository receiptRepository,
            PaymentReceiptStorage storage,
            ManualPaymentReceiptResultMapper mapper
    ) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.storage = Objects.requireNonNull(storage, "storage must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public ManualPaymentReceiptContentResult getContent(GetManualPaymentReceiptContentQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        ManualPaymentReceipt receipt = receiptRepository.findById(Objects.requireNonNull(query.receiptId(), "receiptId must not be null"))
                .orElseThrow(() -> new ManualPaymentReceiptNotFoundException(query.receiptId()));
        if (!receipt.hasStoredContent()) {
            throw new ManualPaymentReceiptContentUnavailableException();
        }
        PaymentReceiptContent content = storage.load(receipt.getStorageKey());
        return mapper.toContentResult(content);
    }
}
