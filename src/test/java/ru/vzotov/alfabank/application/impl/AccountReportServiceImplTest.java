package ru.vzotov.alfabank.application.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import ru.vzotov.alfabank.domain.model.AlfabankOperation;
import ru.vzotov.alfabank.domain.model.TransactionId;
import ru.vzotov.accounting.application.AccountNotFoundException;
import ru.vzotov.accounting.application.AccountReportNotFoundException;
import ru.vzotov.accounting.application.AccountingService;
import ru.vzotov.accounting.domain.model.AccountReport;
import ru.vzotov.accounting.domain.model.AccountReportId;
import ru.vzotov.accounting.domain.model.AccountReportRepository;
import ru.vzotov.banking.domain.model.AccountNumber;
import ru.vzotov.banking.domain.model.OperationId;
import ru.vzotov.banking.domain.model.OperationType;
import ru.vzotov.banking.domain.model.TransactionReference;
import ru.vzotov.domain.model.Money;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit4.class)
public class AccountReportServiceImplTest {

    private AccountReportServiceAlfabank service;
    private AccountReportId reportId;
    private AccountReportRepository<AlfabankOperation> reportRepository;
    private AccountingService accountingService;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        reportRepository = Mockito.mock(AccountReportRepository.class);
        accountingService = Mockito.mock(AccountingService.class);
        service = new AccountReportServiceAlfabank(reportRepository, accountingService);
        reportId = new AccountReportId("test-1", LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC));

        List<AlfabankOperation> operations = Collections.singletonList(
                new AlfabankOperation(
                        "test type",
                        "40817810108290123456",
                        "RUR",
                        LocalDate.now(),
                        new TransactionId("test transaction"),
                        "test description",
                        0.0d,
                        123.45d,
                        null
                )
        );

        Mockito.when(reportRepository.find(reportId))
                .thenReturn(new AccountReport<>(reportId, operations));

        Mockito.when(accountingService.registerOperation(
                Mockito.any(AccountNumber.class),
                Mockito.any(LocalDate.class),
                Mockito.any(TransactionReference.class),
                Mockito.any(OperationType.class),
                Mockito.any(Money.class),
                Mockito.anyString()
        )).thenReturn(new OperationId("test-op-1"));
    }

    @Test
    public void processAccountReport() throws AccountReportNotFoundException, AccountNotFoundException {
        service.processAccountReport(new AccountReportId("test-1", LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)));
        Mockito.verify(accountingService).registerOperation(
                new AccountNumber("40817810108290123456"),
                LocalDate.now(),
                new TransactionReference("test transaction"),
                OperationType.WITHDRAW,
                Money.kopecks(12345),
                "test description"
        );
    }

}
