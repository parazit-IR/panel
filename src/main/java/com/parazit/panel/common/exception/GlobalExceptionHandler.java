package com.parazit.panel.common.exception;

import com.parazit.panel.application.plan.catalog.AvailablePlanNotFoundException;
import com.parazit.panel.application.plan.admin.InvalidPlanStateTransitionException;
import com.parazit.panel.application.plan.admin.PlanCodeAlreadyExistsException;
import com.parazit.panel.application.plan.admin.PlanModificationNotAllowedException;
import com.parazit.panel.application.plan.admin.PlanNotFoundException;
import com.parazit.panel.application.plan.selection.PlanSelectionConflictException;
import com.parazit.panel.application.plan.selection.PlanSelectionNotFoundException;
import com.parazit.panel.application.plan.selection.UserNotEligibleForPlanSelectionException;
import com.parazit.panel.application.payment.PaymentConflictException;
import com.parazit.panel.application.payment.PaymentApprovalException;
import com.parazit.panel.application.payment.PaymentNotFoundException;
import com.parazit.panel.application.payment.PaymentOrderNotFoundException;
import com.parazit.panel.application.payment.PaymentProcessorNotFoundException;
import com.parazit.panel.application.payment.manual.ManualCardPaymentDisabledException;
import com.parazit.panel.application.payment.manual.ManualCardPaymentNotAllowedException;
import com.parazit.panel.application.payment.manual.ManualPaymentAmountReservationException;
import com.parazit.panel.application.payment.manual.ManualPaymentDestinationUnavailableException;
import com.parazit.panel.application.payment.manual.ManualPaymentInstructionConflictException;
import com.parazit.panel.application.payment.manual.ManualPaymentInstructionNotFoundException;
import com.parazit.panel.application.payment.manual.ManualPaymentReissueNotAllowedException;
import com.parazit.panel.application.payment.manual.ManualPaymentRequestIdConflictException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptAlreadySubmittedException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptAmountMismatchException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptContentUnavailableException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptDuplicateException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptFileTooLargeException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptInvalidFileException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptNotFoundException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptRequestIdConflictException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptStorageException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptSubmissionNotAllowedException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptUnsupportedTypeException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptWithdrawalNotAllowedException;
import com.parazit.panel.application.payment.manual.review.ManualPaymentReviewConflictException;
import com.parazit.panel.application.payment.manual.review.ManualPaymentReviewNotAllowedException;
import com.parazit.panel.application.payment.manual.review.ManualPaymentReviewNotFoundException;
import com.parazit.panel.application.provisioning.outbox.ProvisioningOutboxException;
import com.parazit.panel.application.provisioning.outbox.ProvisioningOutboxNotFoundException;
import com.parazit.panel.application.provisioning.outbox.ProvisioningOutboxRetryNotAllowedException;
import com.parazit.panel.application.payment.zarinpal.PaymentAlreadyApprovedException;
import com.parazit.panel.application.payment.zarinpal.PaymentVerificationConflictException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalAmountMismatchException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalAuthorityNotFoundException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalCallbackInvalidException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalConfigurationException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalDisabledException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalPaymentNotAllowedException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalRequestFailedException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalRequestUnknownException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalResponseInvalidException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalVerificationFailedException;
import com.parazit.panel.application.payment.zarinpal.ZarinpalVerificationUnknownException;
import com.parazit.panel.application.referral.ReferralAlreadyAssignedException;
import com.parazit.panel.application.referral.ReferralCodeNotFoundException;
import com.parazit.panel.application.referral.SelfReferralNotAllowedException;
import com.parazit.panel.application.subscription.SubscriptionEndpointConfigurationException;
import com.parazit.panel.application.subscription.SubscriptionNotAccessibleException;
import com.parazit.panel.application.subscription.SubscriptionNotFoundException;
import com.parazit.panel.application.subscription.SubscriptionOwnershipException;
import com.parazit.panel.application.subscription.SubscriptionRenderingException;
import com.parazit.panel.application.subscription.SubscriptionTokenInvalidException;
import com.parazit.panel.application.subscription.UnsupportedInboundConfigurationException;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.application.xui.inbound.XuiEligibleInboundNotFoundException;
import com.parazit.panel.application.xui.inbound.XuiInboundAmbiguousException;
import com.parazit.panel.application.xui.inbound.XuiInboundNotFoundException;
import com.parazit.panel.application.xui.client.XuiClientProvisionFailedException;
import com.parazit.panel.application.xui.client.XuiClientProvisionNotAllowedException;
import com.parazit.panel.application.xui.client.XuiClientProvisionNotFoundException;
import com.parazit.panel.application.xui.client.XuiClientProvisionUnknownException;
import com.parazit.panel.application.xui.client.XuiClientDeleteFailedException;
import com.parazit.panel.application.xui.client.XuiClientDeleteNotAllowedException;
import com.parazit.panel.application.xui.client.XuiClientDisableFailedException;
import com.parazit.panel.application.xui.client.XuiClientDisableNotAllowedException;
import com.parazit.panel.application.xui.client.XuiClientOperationUnknownException;
import com.parazit.panel.application.xui.client.XuiClientOperationInProgressException;
import com.parazit.panel.application.xui.client.XuiClientOperationNotAllowedException;
import com.parazit.panel.application.xui.client.XuiClientOperationNotFoundException;
import com.parazit.panel.application.xui.client.XuiInboundNotEligibleException;
import com.parazit.panel.application.xui.client.XuiOperationIdConflictException;
import com.parazit.panel.application.xui.client.XuiProvisionOwnershipException;
import com.parazit.panel.application.xui.client.XuiRemoteClientIdentityMismatchException;
import com.parazit.panel.application.xui.client.XuiRemoteClientMissingException;
import com.parazit.panel.application.xui.client.XuiRemoteStateConflictException;
import com.parazit.panel.application.xui.client.XuiTrafficOverflowException;
import com.parazit.panel.application.xui.client.XuiClientTrafficResetFailedException;
import com.parazit.panel.application.xui.client.XuiClientTrafficResetUnknownException;
import com.parazit.panel.application.xui.client.XuiClientUpdateFailedException;
import com.parazit.panel.application.xui.client.XuiClientUpdateUnknownException;
import com.parazit.panel.infrastructure.xui.exception.XuiAuthenticationException;
import com.parazit.panel.infrastructure.xui.exception.XuiClientException;
import com.parazit.panel.infrastructure.xui.exception.XuiConnectionException;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import com.parazit.panel.infrastructure.xui.exception.XuiServerException;
import com.parazit.panel.infrastructure.xui.exception.XuiTimeoutException;
import com.parazit.panel.infrastructure.security.OperatorIdentityException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        ApiErrorResponse response = buildErrorResponse(HttpStatus.BAD_REQUEST, message, servletRequest);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                servletRequest
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        ApiErrorResponse response = buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Receipt file is too large",
                servletRequest
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNoSuchElement(
            NoSuchElementException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String name = exception.getName();
        return buildResponse(HttpStatus.BAD_REQUEST, name + " has an invalid value", request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        log.debug("User resource not found: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ReferralCodeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleReferralCodeNotFound(
            ReferralCodeNotFoundException exception,
            HttpServletRequest request
    ) {
        log.debug("Referral code not found");
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(SelfReferralNotAllowedException.class)
    public ResponseEntity<ApiErrorResponse> handleSelfReferralNotAllowed(
            SelfReferralNotAllowedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ReferralAlreadyAssignedException.class)
    public ResponseEntity<ApiErrorResponse> handleReferralAlreadyAssigned(
            ReferralAlreadyAssignedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(PlanNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePlanNotFound(
            PlanNotFoundException exception,
            HttpServletRequest request
    ) {
        log.debug("Plan resource not found: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(AvailablePlanNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAvailablePlanNotFound(
            AvailablePlanNotFoundException exception,
            HttpServletRequest request
    ) {
        log.debug("Available plan resource not found: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(PlanSelectionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePlanSelectionNotFound(
            PlanSelectionNotFoundException exception,
            HttpServletRequest request
    ) {
        log.debug("Plan selection resource not found: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({
            XuiInboundNotFoundException.class,
            XuiEligibleInboundNotFoundException.class,
            XuiClientProvisionNotFoundException.class,
            XuiClientOperationNotFoundException.class,
            XuiProvisionOwnershipException.class,
            PaymentNotFoundException.class,
            PaymentOrderNotFoundException.class,
            ManualPaymentInstructionNotFoundException.class,
            ManualPaymentReceiptNotFoundException.class,
            ZarinpalAuthorityNotFoundException.class,
            SubscriptionNotFoundException.class,
            SubscriptionOwnershipException.class
    })
    public ResponseEntity<ApiErrorResponse> handleXuiInboundNotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        log.debug("Xui inbound resource not found: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(UserNotEligibleForPlanSelectionException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotEligibleForPlanSelection(
            UserNotEligibleForPlanSelectionException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler({
            PlanCodeAlreadyExistsException.class,
            PlanModificationNotAllowedException.class,
            InvalidPlanStateTransitionException.class,
            PlanSelectionConflictException.class,
            XuiInboundAmbiguousException.class,
            XuiClientDisableNotAllowedException.class,
            XuiClientDeleteNotAllowedException.class,
            XuiRemoteClientIdentityMismatchException.class,
            XuiOperationIdConflictException.class,
            XuiClientOperationInProgressException.class,
            XuiClientOperationNotAllowedException.class,
            XuiRemoteClientMissingException.class,
            XuiRemoteStateConflictException.class,
            XuiTrafficOverflowException.class,
            PaymentConflictException.class,
            PaymentApprovalException.class,
            PaymentProcessorNotFoundException.class,
            ManualCardPaymentNotAllowedException.class,
            ManualPaymentInstructionConflictException.class,
            ManualPaymentRequestIdConflictException.class,
            ManualPaymentReissueNotAllowedException.class,
            ManualPaymentReceiptSubmissionNotAllowedException.class,
            ManualPaymentReceiptRequestIdConflictException.class,
            ManualPaymentReceiptAlreadySubmittedException.class,
            ManualPaymentReceiptDuplicateException.class,
            ManualPaymentReceiptWithdrawalNotAllowedException.class,
            ManualPaymentReviewConflictException.class,
            ManualPaymentReviewNotAllowedException.class,
            ProvisioningOutboxRetryNotAllowedException.class,
            ZarinpalDisabledException.class,
            ZarinpalConfigurationException.class,
            ZarinpalPaymentNotAllowedException.class,
            ZarinpalAmountMismatchException.class,
            PaymentAlreadyApprovedException.class,
            PaymentVerificationConflictException.class,
            SubscriptionNotAccessibleException.class,
            SubscriptionTokenInvalidException.class
    })
    public ResponseEntity<ApiErrorResponse> handlePlanConflict(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({
            ManualPaymentReceiptAmountMismatchException.class,
            ManualPaymentReceiptInvalidFileException.class
    })
    public ResponseEntity<ApiErrorResponse> handleManualReceiptBadRequest(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ManualPaymentReceiptUnsupportedTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleManualReceiptUnsupportedType(
            ManualPaymentReceiptUnsupportedTypeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getMessage(), request);
    }

    @ExceptionHandler(ManualPaymentReceiptFileTooLargeException.class)
    public ResponseEntity<ApiErrorResponse> handleManualReceiptTooLarge(
            ManualPaymentReceiptFileTooLargeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Receipt file is too large", request);
    }

    @ExceptionHandler({
            ManualPaymentReviewNotFoundException.class,
            ProvisioningOutboxNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleTask31NotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(OperatorIdentityException.class)
    public ResponseEntity<ApiErrorResponse> handleOperatorIdentity(
            OperatorIdentityException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(ProvisioningOutboxException.class)
    public ResponseEntity<ApiErrorResponse> handleProvisioningOutbox(
            ProvisioningOutboxException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedInboundConfigurationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedSubscriptionInbound(
            UnsupportedInboundConfigurationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler({
            SubscriptionRenderingException.class,
            SubscriptionEndpointConfigurationException.class
    })
    public ResponseEntity<ApiErrorResponse> handleSubscriptionUnavailable(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler({
            ManualPaymentReceiptStorageException.class,
            ManualPaymentReceiptContentUnavailableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleManualReceiptStorage(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler({
            ManualCardPaymentDisabledException.class,
            ManualPaymentDestinationUnavailableException.class,
            ManualPaymentAmountReservationException.class
    })
    public ResponseEntity<ApiErrorResponse> handleManualPaymentUnavailable(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler({
            XuiClientProvisionNotAllowedException.class,
            XuiInboundNotEligibleException.class
    })
    public ResponseEntity<ApiErrorResponse> handleXuiProvisionConflict(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(ZarinpalCallbackInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleZarinpalInvalidCallback(
            ZarinpalCallbackInvalidException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler({
            ZarinpalRequestFailedException.class,
            ZarinpalVerificationFailedException.class
    })
    public ResponseEntity<ApiErrorResponse> handleZarinpalGatewayRejected(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(ZarinpalResponseInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleZarinpalInvalidResponse(
            ZarinpalResponseInvalidException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler({
            ZarinpalRequestUnknownException.class,
            ZarinpalVerificationUnknownException.class
    })
    public ResponseEntity<ApiErrorResponse> handleZarinpalUnknown(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(XuiClientProvisionFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleXuiProvisionFailed(
            XuiClientProvisionFailedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler({
            XuiClientDisableFailedException.class,
            XuiClientDeleteFailedException.class,
            XuiClientUpdateFailedException.class,
            XuiClientTrafficResetFailedException.class
    })
    public ResponseEntity<ApiErrorResponse> handleXuiLifecycleFailed(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(XuiClientProvisionUnknownException.class)
    public ResponseEntity<ApiErrorResponse> handleXuiProvisionUnknown(
            XuiClientProvisionUnknownException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler({
            XuiClientOperationUnknownException.class,
            XuiClientUpdateUnknownException.class,
            XuiClientTrafficResetUnknownException.class
    })
    public ResponseEntity<ApiErrorResponse> handleXuiLifecycleUnknown(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(XuiInvalidResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleXuiInvalidResponse(
            XuiInvalidResponseException exception,
            HttpServletRequest request
    ) {
        log.warn("Xui invalid response: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler({
            XuiServerException.class,
            XuiAuthenticationException.class,
            XuiClientException.class
    })
    public ResponseEntity<ApiErrorResponse> handleXuiRemoteFailure(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        log.warn("Xui remote failure: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
    }

    @ExceptionHandler(XuiConnectionException.class)
    public ResponseEntity<ApiErrorResponse> handleXuiConnection(
            XuiConnectionException exception,
            HttpServletRequest request
    ) {
        log.warn("Xui connection failure: {}", exception.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(XuiTimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handleXuiTimeout(
            XuiTimeoutException exception,
            HttpServletRequest request
    ) {
        log.warn("Xui timeout: {}", exception.getMessage());
        return buildResponse(HttpStatus.GATEWAY_TIMEOUT, exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        log.error("Unexpected runtime exception", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unexpected exception", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                sanitizeMessage(message, status),
                request.getRequestURI(),
                MDC.get(TraceIdFilter.TRACE_ID_KEY)
        );
        return ResponseEntity.status(status).body(response);
    }

    private ApiErrorResponse buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                sanitizeMessage(message, status),
                request.getRequestURI(),
                MDC.get(TraceIdFilter.TRACE_ID_KEY)
        );
    }

    private String sanitizeMessage(String message, HttpStatus status) {
        if (message == null || message.isBlank()) {
            return status.getReasonPhrase();
        }
        return message;
    }
}
