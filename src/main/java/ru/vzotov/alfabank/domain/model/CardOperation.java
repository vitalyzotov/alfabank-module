package ru.vzotov.alfabank.domain.model;

import org.apache.commons.math3.util.Precision;
import ru.vzotov.ddd.shared.ValueObject;

import java.time.LocalDate;
import java.util.Objects;

public class CardOperation implements ValueObject<CardOperation> {

    private final String cardNumber;

    private final PosInfo posInfo;

    private final LocalDate authDate;

    private final LocalDate purchaseDate;

    private final Double amount;

    private final String currency;

    private final String extraInfo;

    private final String mcc;

    public CardOperation(String cardNumber, PosInfo posInfo, LocalDate authDate, LocalDate purchaseDate, Double amount, String currency,
                         String extraInfo, String mcc) {
        this.cardNumber = cardNumber;
        this.posInfo = posInfo;
        this.authDate = authDate;
        this.purchaseDate = purchaseDate;
        this.amount = amount;
        this.currency = currency;
        this.extraInfo = extraInfo;
        this.mcc = mcc;
    }

    public String cardNumber() {
        return cardNumber;
    }

    public PosInfo posInfo() {
        return posInfo;
    }

    public LocalDate authDate() {
        return authDate;
    }

    public LocalDate purchaseDate() {
        return purchaseDate;
    }

    public Double amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public String extraInfo() {
        return extraInfo;
    }

    public String mcc() {
        return mcc;
    }

    @Override
    public boolean sameValueAs(CardOperation that) {
        return that != null && Objects.equals(cardNumber, that.cardNumber) &&
                Objects.equals(posInfo, that.posInfo) &&
                Objects.equals(authDate, that.authDate) &&
                Objects.equals(purchaseDate, that.purchaseDate) &&
                Precision.equals(amount, that.amount, 0.0001d) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(extraInfo, that.extraInfo) &&
                Objects.equals(mcc, that.mcc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardOperation that = (CardOperation) o;
        return sameValueAs(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, posInfo, authDate, purchaseDate, amount, currency, extraInfo, mcc);
    }

    @Override
    public String toString() {
        return "CardOperation{" +
                "cardNumber='" + cardNumber + '\'' +
                ", posInfo=" + posInfo +
                ", authDate=" + authDate +
                ", purchaseDate=" + purchaseDate +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", extraInfo='" + extraInfo + '\'' +
                ", mcc='" + mcc + '\'' +
                '}';
    }
}
