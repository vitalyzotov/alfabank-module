package ru.vzotov.alfabank.domain.model;

import ru.vzotov.ddd.shared.ValueObject;

import java.util.Objects;

public record PosInfo(
        String terminalId,
        String country,
        String city,
        String street,
        String merchant) implements ValueObject<PosInfo> {

    @Override
    public boolean sameValueAs(PosInfo posInfo) {
        return posInfo != null && Objects.equals(terminalId, posInfo.terminalId) &&
                Objects.equals(country, posInfo.country) &&
                Objects.equals(city, posInfo.city) &&
                Objects.equals(street, posInfo.street) &&
                Objects.equals(merchant, posInfo.merchant);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PosInfo posInfo = (PosInfo) o;
        return sameValueAs(posInfo);
    }

    @Override
    public String toString() {
        return "PosInfo{" +
                "terminalId='" + terminalId + '\'' +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", street='" + street + '\'' +
                ", merchant='" + merchant + '\'' +
                '}';
    }
}
