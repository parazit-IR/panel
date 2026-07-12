package com.parazit.panel.application.payment.manual.result;

public final class InitializeManualCardPaymentResult {

    private final ManualCardPaymentInstructionResult instruction;

    public InitializeManualCardPaymentResult(ManualCardPaymentInstructionResult instruction) {
        this.instruction = instruction;
    }

    public ManualCardPaymentInstructionResult instruction() {
        return instruction;
    }

    public boolean newlyInitialized() {
        return instruction.newlyInitialized();
    }

    @Override
    public String toString() {
        return "InitializeManualCardPaymentResult[instruction=" + instruction + ']';
    }
}
