package ru.vzotov.alfabank.domain.model;

import org.apache.commons.lang.Validate;
import ru.vzotov.accounting.domain.model.AccountReportOperation;
import ru.vzotov.ddd.shared.ValueObject;

import java.time.LocalDate;
import java.util.Objects;

public record AlfabankOperation(
        String accountType,
        String accountNumber,
        String currencyCode,
        LocalDate date,
        TransactionId transactionId,
        String description,
        Double deposit,
        Double withdraw,
        CardOperation cardOperation) implements ValueObject<AlfabankOperation>, AccountReportOperation {

    public AlfabankOperation {
        Validate.notNull(accountType);
        Validate.notNull(accountNumber);
        Validate.notNull(currencyCode);
        Validate.notNull(date);
        Validate.notNull(transactionId);
        Validate.notNull(description);
        Validate.notNull(deposit);
        Validate.notNull(withdraw);

    }

    @Override
    public boolean sameValueAs(AlfabankOperation that) {
        return that != null && Objects.equals(accountType, that.accountType) &&
                Objects.equals(accountNumber, that.accountNumber) &&
                Objects.equals(currencyCode, that.currencyCode) &&
                Objects.equals(date, that.date) &&
                Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(description, that.description) &&
                Objects.equals(deposit, that.deposit) &&
                Objects.equals(withdraw, that.withdraw) &&
                Objects.equals(cardOperation, that.cardOperation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlfabankOperation that = (AlfabankOperation) o;
        return sameValueAs(that);
    }

}
