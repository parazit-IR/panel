package com.parazit.panel.api.internal.user;

import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.user.result.RegisterUserResult;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary internal endpoint for verifying Telegram user registration.
 * It should be replaced by the Telegram bot adapter in a later task.
 */
@RestController
@RequestMapping("/internal/users")
public class RegisterUserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final RegisterUserApiMapper mapper;

    public RegisterUserController(
            RegisterUserUseCase registerUserUseCase,
            RegisterUserApiMapper mapper
    ) {
        this.registerUserUseCase = Objects.requireNonNull(registerUserUseCase, "registerUserUseCase must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        RegisterUserResult result = registerUserUseCase.register(mapper.toCommand(request));
        HttpStatus status = result.newlyCreated() ? HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity.status(status)
                .body(mapper.toResponse(result));
    }
}
