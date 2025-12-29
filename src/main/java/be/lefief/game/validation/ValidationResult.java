package be.lefief.game.validation;

import lombok.Getter;

/**
 * Result of a command validation check.
 */
@Getter
public class ValidationResult {

    private final boolean valid;
    private final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }

    public boolean isInvalid() {
        return !valid;
    }
}
