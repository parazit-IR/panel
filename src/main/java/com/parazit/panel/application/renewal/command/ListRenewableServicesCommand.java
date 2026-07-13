package com.parazit.panel.application.renewal.command;

public record ListRenewableServicesCommand(long telegramUserId, int page, int size) {
}
