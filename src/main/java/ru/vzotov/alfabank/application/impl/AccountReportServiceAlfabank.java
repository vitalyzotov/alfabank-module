package ru.vzotov.alfabank.application.impl;

import ru.vzotov.alfabank.domain.model.AlfabankOperation;
import ru.vzotov.alfabank.domain.model.CardOperation;
import ru.vzotov.alfabank.domain.model.PosInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.vzotov.accounting.application.AccountNotFoundException;
import ru.vzotov.accounting.application.AccountReportNotFoundException;
import ru.vzotov.accounting.application.AccountReportService;
import ru.vzotov.accounting.application.AccountingService;
import ru.vzotov.accounting.domain.model.AccountReport;
import ru.vzotov.accounting.domain.model.AccountReportId;
import ru.vzotov.accounting.domain.model.AccountReportRepository;
import ru.vzotov.banking.domain.model.AccountNumber;
import ru.vzotov.banking.domain.model.BankId;
import ru.vzotov.banking.domain.model.CardNumber;
import ru.vzotov.banking.domain.model.City;
import ru.vzotov.banking.domain.model.Country;
import ru.vzotov.banking.domain.model.MccCode;
import ru.vzotov.banking.domain.model.Merchant;
import ru.vzotov.banking.domain.model.OperationId;
import ru.vzotov.banking.domain.model.OperationType;
import ru.vzotov.banking.domain.model.PosTerminal;
import ru.vzotov.banking.domain.model.PosTerminalId;
import ru.vzotov.banking.domain.model.Street;
import ru.vzotov.banking.domain.model.TransactionReference;
import ru.vzotov.domain.model.Money;

import java.io.IOException;
import java.io.InputStream;
import java.util.Currency;
import java.util.List;

import static ru.vzotov.banking.domain.model.OperationType.DEPOSIT;
import static ru.vzotov.banking.domain.model.OperationType.WITHDRAW;

@Service
@Qualifier("AccountReportServiceAlfabank")
public class AccountReportServiceAlfabank implements AccountReportService {

    private static final Logger log = LoggerFactory.getLogger(AccountReportServiceAlfabank.class);

    private final AccountReportRepository<AlfabankOperation> accountReportRepository;

    private final AccountingService accountingService;

    AccountReportServiceAlfabank(
            @Autowired @Qualifier("accountReportRepositoryAlfabank") AccountReportRepository<AlfabankOperation> accountReportRepository,
            @Autowired AccountingService accountingService) {
        this.accountReportRepository = accountReportRepository;
        this.accountingService = accountingService;
    }

    @Override
    public BankId bankId() {
        return BankId.ALFABANK;
    }

    @Override
    public AccountReportId save(String name, InputStream content) throws IOException {
        Validate.notNull(name);
        Validate.notNull(content);
        return accountReportRepository.save(name, content);
    }

    @Override
    public void processAccountReport(AccountReportId reportId) throws AccountReportNotFoundException, AccountNotFoundException {
        Validate.notNull(reportId);

        final AccountReport<AlfabankOperation> report = accountReportRepository.find(reportId);
        if (report == null) {
            throw new AccountReportNotFoundException();
        }

        for (AlfabankOperation row : report.operations()) {
            final OperationType type = row.withdraw() > row.deposit() ? WITHDRAW : DEPOSIT;

            //noinspection ConstantConditions
            Validate.isTrue(WITHDRAW.equals(type) || DEPOSIT.equals(type));
            Validate.isTrue(DEPOSIT.equals(type) || Precision.equals(row.deposit(), 0.00d, 0.001d));
            Validate.isTrue(WITHDRAW.equals(type) || Precision.equals(row.withdraw(), 0.00d, 0.001d));

            final AccountNumber accountNumber = new AccountNumber(row.accountNumber());
            final Currency currency = Currency.getInstance(row.currencyCode());
            final Money amount = new Money(DEPOSIT.equals(type) ? row.deposit() : row.withdraw(), currency);

            // Skip HOLD records.
            // The records are not yet completed operations.
            if (row.transactionId().isHold()) {
                accountingService.registerHoldOperation(
                        accountNumber,
                        row.date(),
                        type,
                        amount,
                        row.description()
                );
            } else {
                OperationId operationId = accountingService.registerOperation(
                        accountNumber,
                        row.date(),
                        new TransactionReference(row.transactionId().reference()),
                        type,
                        amount,
                        row.description()
                );

                final CardOperation card = row.cardOperation();
                if (card != null) {
                    accountingService.registerCardOperation(
                            operationId,
                            new CardNumber(card.cardNumber()),
                            makePosTerminal(card.posInfo()),
                            card.authDate(),
                            card.purchaseDate(),
                            new Money(card.amount(), Currency.getInstance(card.currency())),
                            card.extraInfo(),
                            new MccCode(card.mcc())
                    );
                }

                accountingService.removeMatchingHoldOperations(operationId);
            }
        }

        accountReportRepository.markProcessed(reportId);
    }

    public static PosTerminal makePosTerminal(PosInfo posInfo) {
        Country country;
        try {
            country = new Country(posInfo.country());
        }catch (IllegalArgumentException | NullPointerException ex) {
            country = null;
        }
        return country == null ? null : new PosTerminal(
                new PosTerminalId(posInfo.terminalId()),
                country,
                StringUtils.isEmpty(posInfo.city()) ? null : new City(posInfo.city()),
                StringUtils.isEmpty(posInfo.street()) ? null : new Street(posInfo.street()),
                new Merchant(posInfo.merchant())
        );
    }

    @Override
    public void processNewReports() {
        List<AccountReportId> reports = accountReportRepository.findUnprocessed();

        log.info("Found {} unprocessed reports", reports.size());

        for (AccountReportId reportId : reports) {
            log.info("Start processing of report {}", reportId);
            try {
                processAccountReport(reportId);

                log.info("Processing of report {} finished", reportId);
            } catch (AccountReportNotFoundException | AccountNotFoundException e) {
                log.warn("Processing failed for report {}", reportId);
            }
        }

    }
}
