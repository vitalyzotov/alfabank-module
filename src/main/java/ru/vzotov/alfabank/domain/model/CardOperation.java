package ru.vzotov.alfabank.domain.model;

import org.apache.commons.math3.util.Precision;
import ru.vzotov.ddd.shared.ValueObject;

import java.time.LocalDate;
import java.util.Objects;

public record CardOperation(
        String cardNumber,
        PosInfo posInfo,
        LocalDate authDate,
        LocalDate purchaseDate,
        Double amount,
        String currency,
        String extraInfo,
        String mcc) implements ValueObject<CardOperation> {

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
