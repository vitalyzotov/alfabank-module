package ru.vzotov.alfabank.domain.model;

import ru.vzotov.ddd.shared.ValueObject;

import java.util.Objects;

public class PosInfo implements ValueObject<PosInfo> {

    private final String terminalId;

    private final String country;

    private final String city;

    private final String street;

    private final String merchant;

    public PosInfo(String terminalId, String country, String city, String street, String merchant) {
        this.terminalId = terminalId;
        this.country = country;
        this.city = city;
        this.street = street;
        this.merchant = merchant;
    }

    public String terminalId() {
        return terminalId;
    }

    public String country() {
        return country;
    }

    public String city() {
        return city;
    }

    public String street() {
        return street;
    }

    public String merchant() {
        return merchant;
    }

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
    public int hashCode() {
        return Objects.hash(terminalId, country, city, street, merchant);
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
