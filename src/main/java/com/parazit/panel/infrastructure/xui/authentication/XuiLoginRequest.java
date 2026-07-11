package com.parazit.panel.infrastructure.xui.authentication;

public record XuiLoginRequest(
        String username,
        String password
) {
}
