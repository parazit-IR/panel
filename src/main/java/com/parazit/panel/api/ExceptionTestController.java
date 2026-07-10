package com.parazit.panel.api;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/_test/exceptions")
public class ExceptionTestController {

    @GetMapping("/runtime")
    public void runtimeException() {
        throw new RuntimeException("Sample runtime exception");
    }

    @GetMapping("/illegal-argument")
    public void illegalArgumentException() {
        throw new IllegalArgumentException("Sample illegal argument");
    }

    @GetMapping("/not-found")
    public void noSuchElementException() {
        throw new NoSuchElementException("Sample resource not found");
    }

    @GetMapping("/access-denied")
    public void accessDeniedException() {
        throw new AccessDeniedException("Sample access denied");
    }

    @GetMapping("/constraint-violation")
    public void constraintViolationException() {
        throw new ConstraintViolationException("Sample constraint violation", Set.of());
    }

    @GetMapping("/method-argument-not-valid")
    public void methodArgumentNotValidException() throws NoSuchMethodException, MethodArgumentNotValidException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new SampleRequest(""), "sampleRequest");
        bindingResult.rejectValue("value", "NotBlank", "must not be blank");
        Method method = ExceptionTestController.class.getDeclaredMethod("sampleMethodArgument", SampleRequest.class);

        throw new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }

    @SuppressWarnings("unused")
    private void sampleMethodArgument(SampleRequest request) {
    }

    public record SampleRequest(
            @NotBlank String value
    ) {
    }
}
