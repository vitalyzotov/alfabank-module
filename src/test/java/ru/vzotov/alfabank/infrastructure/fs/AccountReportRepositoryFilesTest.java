package ru.vzotov.alfabank.infrastructure.fs;

import ru.vzotov.alfabank.application.impl.AccountReportServiceAlfabank;
import ru.vzotov.alfabank.domain.model.AlfabankOperation;
import ru.vzotov.alfabank.domain.model.CardOperation;
import ru.vzotov.alfabank.domain.model.PosInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vzotov.accounting.domain.model.AccountReport;
import ru.vzotov.accounting.domain.model.AccountReportId;
import ru.vzotov.banking.domain.model.CardNumber;
import ru.vzotov.banking.domain.model.City;
import ru.vzotov.banking.domain.model.Country;
import ru.vzotov.banking.domain.model.Merchant;
import ru.vzotov.banking.domain.model.PosTerminal;
import ru.vzotov.banking.domain.model.PosTerminalId;
import ru.vzotov.banking.domain.model.Street;

import java.io.File;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class AccountReportRepositoryFilesTest {

    private static final Logger log = LoggerFactory.getLogger(AccountReportRepositoryFilesTest.class);

    @Test
    public void find() {
        File resourcesDirectory = new File("src/test/resources/account-reports");
        AlfabankReportRepositoryFiles repo = new AlfabankReportRepositoryFiles(resourcesDirectory.getAbsolutePath());
        List<AccountReportId> all = repo.findAll();
        List<AccountReport<AlfabankOperation>> reports = new ArrayList<>();
        for (AccountReportId id : all) {
            reports.add(repo.find(id));
        }
        assertThat(reports).hasSize(3);

        /////////////////////////
        List<AlfabankOperation> operations = reports.stream()
                .filter(report -> report.reportId().name().equalsIgnoreCase("credit_movementList_2018-06.csv"))
                .flatMap(report -> report.operations().stream())
                .collect(Collectors.toList());
        assertThat(operations).hasSize(26);

        AlfabankOperation operation = operations.get(0);
        assertThat(operation.date()).isEqualTo(LocalDate.of(2018, 6, 25));
        assertThat(operation.deposit()).isEqualTo(0.0d);
        assertThat(operation.withdraw()).isEqualTo(33199.94d);

        //operations = reports.get(1).operations();
        for (AlfabankOperation row : operations) {
            final CardOperation card = row.cardOperation();
            if (card != null) {
                log.info("Try to process operation {}", card);

                CardNumber cardNumber = new CardNumber(card.cardNumber());
                assertThat(cardNumber.value()).isNotEmpty();

                PosTerminal posTerminal = AccountReportServiceAlfabank.makePosTerminal(card.posInfo());
                assertThat(posTerminal.country()).isNotNull();
            }
        }

        /////////////////////////
        operations = reports.stream()
                .filter(report -> report.reportId().name().equalsIgnoreCase("debit_movementList_2020_2020-03-30.csv"))
                .flatMap(report -> report.operations().stream())
                .collect(Collectors.toList());
        assertThat(operations).hasSize(235);
        for (AlfabankOperation row : operations) {
            final CardOperation card = row.cardOperation();
            if (card != null) {
                log.info("Try to process operation {}", card);

                CardNumber cardNumber = new CardNumber(card.cardNumber());
                assertThat(cardNumber.value()).isNotEmpty();

                PosTerminal posTerminal = AccountReportServiceAlfabank.makePosTerminal(card.posInfo());
                if (posTerminal != null) {
                    assertThat(posTerminal.country()).isNotNull();

                    if (posTerminal.country() != null) {
                        assertThat(posTerminal.country().code()).isNotNull();
                    }
                }
            }
        }
    }

    @Test
    public void parseCardOperationDescription() throws ParseException {
        CardOperation op = AlfabankReportRepositoryFiles.parseCardOperationDescription("555957++++++1234    10705017\\RUS\\MOSCOW\\1 YA T\\ROSTELECOM             10.07.18 07.07.18       500.00  RUR MCC4812");
        assertThat(op.cardNumber()).isEqualTo("555957++++++1234");
        assertThat(op.posInfo().terminalId()).isEqualTo("10705017");
        assertThat(op.posInfo().country()).isEqualTo("RUS");
        assertThat(op.posInfo().city()).isEqualTo("MOSCOW");
        assertThat(op.posInfo().street()).isEqualTo("1 YA T");
        assertThat(op.posInfo().merchant()).isEqualTo("ROSTELECOM");
        assertThat(op.mcc()).isEqualTo("4812");
        assertThat(op.extraInfo()).isNull();

        op = AlfabankReportRepositoryFiles.parseCardOperationDescription("555957++++++1234    11276718\\RUS\\SARATOV\\6 MOT\\MAGNIT MM ANT          11.07.18 08.07.18      2007.30  RUR (Google pay-9313) MCC5411");
        assertThat(op.cardNumber()).isEqualTo("555957++++++1234");
        assertThat(op.posInfo().terminalId()).isEqualTo("11276718");
        assertThat(op.posInfo().country()).isEqualTo("RUS");
        assertThat(op.posInfo().city()).isEqualTo("SARATOV");
        assertThat(op.posInfo().street()).isEqualTo("6 MOT");
        assertThat(op.posInfo().merchant()).isEqualTo("MAGNIT MM ANT");
        assertThat(op.extraInfo()).isEqualTo("(Google pay-9313)");
        assertThat(op.mcc()).isEqualTo("5411");

        op = AlfabankReportRepositoryFiles.parseCardOperationDescription("415482++++++1234    5101831 /RU/UBRR>Visa Direct                      06.08.18 06.08.18 2040.00       RUR MCC6012");
        assertThat(op.cardNumber()).isEqualTo("415482++++++1234");
        assertThat(op.posInfo().terminalId()).isEqualTo("5101831");
        assertThat(op.posInfo().country()).isEqualTo("RU");
        assertThat(op.posInfo().city()).isNull();
        assertThat(op.posInfo().street()).isNull();
        assertThat(op.posInfo().merchant()).isEqualTo("UBRR>Visa Direct");
        assertThat(op.extraInfo()).isNull();
        assertThat(op.mcc()).isEqualTo("6012");
    }

    @Test
    public void parsePosInfo() {
        PosInfo info;
        PosTerminal terminal;

        info = AlfabankReportRepositoryFiles.parsePosInfo("10705017\\RUS\\MOSCOW\\1 YA T\\ROSTELECOM");
        terminal = AccountReportServiceAlfabank.makePosTerminal(info);
        assertThat(terminal).isEqualTo(new PosTerminal(new PosTerminalId("10705017"), new Country("RUS"),
                new City("MOSCOW"), new Street("1 YA T"), new Merchant("ROSTELECOM")));

        info = AlfabankReportRepositoryFiles.parsePosInfo("11276718\\RUS\\SARATOV\\6 MOT\\MAGNIT MM ANT");
        terminal = AccountReportServiceAlfabank.makePosTerminal(info);
        assertThat(terminal).isEqualTo(new PosTerminal(new PosTerminalId("11276718"), new Country("RUS"),
                new City("SARATOV"), new Street("6 MOT"), new Merchant("MAGNIT MM ANT")));

        info = AlfabankReportRepositoryFiles.parsePosInfo("502546\\643\\SARATOV\\Alfa Iss");
        terminal = AccountReportServiceAlfabank.makePosTerminal(info);
        assertThat(terminal).isEqualTo(new PosTerminal(new PosTerminalId("502546"), new Country("643"),
                new City("SARATOV"), null, new Merchant("Alfa Iss")));

        info = AlfabankReportRepositoryFiles.parsePosInfo("583797\\RUS\\MOSCOW\\GONCHA\\GOSUSLUGI RU");
        terminal = AccountReportServiceAlfabank.makePosTerminal(info);
        assertThat(terminal).isEqualTo(new PosTerminal(new PosTerminalId("583797"), new Country("RUS"),
                new City("MOSCOW"), new Street("GONCHA"), new Merchant("GOSUSLUGI RU")));

        info = AlfabankReportRepositoryFiles.parsePosInfo("23283108\\643\\www delivery \\delivery club");
        terminal = AccountReportServiceAlfabank.makePosTerminal(info);
        assertThat(terminal).isEqualTo(new PosTerminal(new PosTerminalId("23283108"), new Country("643"),
                new City("www delivery"), null, new Merchant("delivery club")));

        info = AlfabankReportRepositoryFiles.parsePosInfo("33331083\\RUS\\MOSCOW\\WWW PAY MTS R");
        terminal = AccountReportServiceAlfabank.makePosTerminal(info);
        assertThat(terminal).isEqualTo(new PosTerminal(new PosTerminalId("33331083"), new Country("RUS"),
                new City("MOSCOW"), null, new Merchant("WWW PAY MTS R")));

        info = AlfabankReportRepositoryFiles.parsePosInfo("W0004183\\RUS\\SARATOV\\PROSP\\MV 142");
        terminal = AccountReportServiceAlfabank.makePosTerminal(info);
        assertThat(terminal).isEqualTo(new PosTerminal(new PosTerminalId("W0004183"), new Country("RUS"),
                new City("SARATOV"), new Street("PROSP"), new Merchant("MV 142")));

        info = AlfabankReportRepositoryFiles.parsePosInfo("RU200213\\RUS\\BORISOGLEBSK\\\\LUKOIL AZS 36");
        terminal = AccountReportServiceAlfabank.makePosTerminal(info);
        assertThat(terminal).isEqualTo(new PosTerminal(new PosTerminalId("RU200213"), new Country("RUS"),
                new City("BORISOGLEBSK"), null, new Merchant("LUKOIL AZS 36")));
    }
}
