package ru.vzotov.alfabank.domain.model;

import org.apache.commons.lang3.Validate;
import ru.vzotov.ddd.shared.ValueObject;

import java.util.Objects;

public record TransactionId(String reference) implements ValueObject<TransactionId> {

    private static final String HOLD = "HOLD";

    public TransactionId {
        Validate.notNull(reference);
    }

    public boolean isHold() {
        return HOLD.equalsIgnoreCase(reference);
    }

    @Override
    public boolean sameValueAs(TransactionId transactionId) {
        return transactionId != null
                && Objects.equals(this.reference, transactionId.reference);
    }
}
