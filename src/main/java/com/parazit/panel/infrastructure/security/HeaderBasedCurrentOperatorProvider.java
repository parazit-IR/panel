package com.parazit.panel.infrastructure.security;

import com.parazit.panel.application.port.out.security.CurrentOperatorProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class HeaderBasedCurrentOperatorProvider implements CurrentOperatorProvider {

    public static final String OPERATOR_HEADER = "X-OPERATOR-ID";
    private static final int MAX_LENGTH = 128;
    private static final Pattern SAFE_OPERATOR_ID = Pattern.compile("[A-Za-z0-9._@:-]{1,128}");

    @Override
    public String currentOperatorId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new OperatorIdentityException("Operator identity is required");
        }
        HttpServletRequest request = attributes.getRequest();
        String operatorId = Objects.toString(request.getHeader(OPERATOR_HEADER), "").trim();
        if (operatorId.isBlank()) {
            throw new OperatorIdentityException("Operator identity is required");
        }
        if (operatorId.length() > MAX_LENGTH || !SAFE_OPERATOR_ID.matcher(operatorId).matches()) {
            throw new OperatorIdentityException("Operator identity is invalid");
        }
        return operatorId;
    }
}
