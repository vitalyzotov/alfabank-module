package ru.vzotov.alfabank.domain.model;

import org.apache.commons.lang.Validate;
import ru.vzotov.accounting.domain.model.AccountReportOperation;
import ru.vzotov.ddd.shared.ValueObject;

import java.time.LocalDate;
import java.util.Objects;

public class AlfabankOperation implements ValueObject<AlfabankOperation>, AccountReportOperation {

    private final String accountType;
    private final String accountNumber;
    private final String currencyCode;
    private final LocalDate date;
    private final TransactionId transactionId;
    private final String description;
    private final Double deposit;
    private final Double withdraw;
    private final CardOperation cardOperation;

    public AlfabankOperation(String accountType, String accountNumber, String currencyCode, LocalDate date, TransactionId transactionId, String description, Double deposit, Double withdraw, CardOperation cardOperation) {
        Validate.notNull(accountType);
        Validate.notNull(accountNumber);
        Validate.notNull(currencyCode);
        Validate.notNull(date);
        Validate.notNull(transactionId);
        Validate.notNull(description);
        Validate.notNull(deposit);
        Validate.notNull(withdraw);

        this.accountType = accountType;
        this.accountNumber = accountNumber;
        this.currencyCode = currencyCode;
        this.date = date;
        this.transactionId = transactionId;
        this.description = description;
        this.deposit = deposit;
        this.withdraw = withdraw;
        this.cardOperation = cardOperation;
    }

    public String accountType() {
        return accountType;
    }

    public String accountNumber() {
        return accountNumber;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public LocalDate date() {
        return date;
    }

    public TransactionId transactionId() {
        return transactionId;
    }

    public String description() {
        return description;
    }

    public Double deposit() {
        return deposit;
    }

    public Double withdraw() {
        return withdraw;
    }

    public CardOperation cardOperation() {
        return cardOperation;
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

    @Override
    public int hashCode() {
        return Objects.hash(accountType, accountNumber, currencyCode, date, transactionId, description, deposit, withdraw, cardOperation);
    }
}
