package ru.vzotov.alfabank.domain.model;

import org.apache.commons.lang.Validate;
import ru.vzotov.ddd.shared.ValueObject;

import java.util.Objects;

public class TransactionId implements ValueObject<TransactionId> {

    private static final String HOLD = "HOLD";

    private final String reference;

    public TransactionId(String reference) {
        Validate.notNull(reference);
        this.reference = reference;
    }

    public String reference() {
        return reference;
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
