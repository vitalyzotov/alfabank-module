package ru.vzotov.alfabank.infrastructure.fs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vzotov.alfabank.domain.model.AlfabankOperation;
import ru.vzotov.alfabank.domain.model.CardOperation;
import ru.vzotov.alfabank.domain.model.PosInfo;
import ru.vzotov.alfabank.domain.model.TransactionId;
import ru.vzotov.accounting.domain.model.AccountReport;
import ru.vzotov.accounting.domain.model.AccountReportId;
import ru.vzotov.accounting.domain.model.AccountReportRepository;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

public class AlfabankReportRepositoryFiles implements AccountReportRepository<AlfabankOperation> {

    private static final Logger log = LoggerFactory.getLogger(AccountReportRepository.class);

    private static final String REPORT_EXT = ".csv";
    private static final String REPORT_PROCESSED_EXT = "_processed.csv";
    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('.')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('.')
            .appendValueReduced(YEAR, 2, 2, 2000)
            .toFormatter();

    private static final Function<File, AccountReportId> MAPPER = file -> {
        try {
            BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

            return new AccountReportId(file.getName(), basicFileAttributes.creationTime().toInstant());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    };

    private final String baseDirectoryPath;

    private final File baseDirectory;


    public AlfabankReportRepositoryFiles(String baseDirectoryPath) {
        this.baseDirectoryPath = baseDirectoryPath;
        this.baseDirectory = new File(baseDirectoryPath);

        Validate.isTrue(this.baseDirectory.isDirectory());
        Validate.isTrue(this.baseDirectory.canRead());
    }

    protected String getBaseDirectoryPath() {
        return baseDirectoryPath;
    }

    protected File getBaseDirectory() {
        return baseDirectory;
    }

    @Override
    public AccountReport<AlfabankOperation> find(AccountReportId reportId) {
        Validate.notNull(reportId);
        final File reportFile = new File(this.getBaseDirectory(), reportId.name());
        Validate.isTrue(reportFile.exists() && reportFile.canRead());


        try (Reader in = new InputStreamReader(new FileInputStream(reportFile), Charset.forName("Cp1251"))) {
            final CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.Predefined.Default.getFormat())
                    .setDelimiter(';')
                    .setTrailingDelimiter(true)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(in);
            final List<AlfabankOperation> operations = StreamSupport.stream(records.spliterator(), false).map(record -> {
                try {
                    final DecimalFormat decimals = createDecimalFormat(new Locale("ru"));
                    final String accountType = record.get("Тип счёта");
                    final String accountNumber = record.get("Номер счета");
                    final String currencyCode = record.get("Валюта");
                    final LocalDate date = LocalDate.parse(record.get("Дата операции"), DATE_FORMAT);
                    final TransactionId transactionId = new TransactionId(record.get("Референс проводки"));
                    final String description = record.get("Описание операции");
                    final Double deposit = decimals.parse(record.get("Приход")).doubleValue();
                    final Double withdraw = decimals.parse(record.get("Расход")).doubleValue();
                    final CardOperation card = parseCardOperationDescription(description);

                    return new AlfabankOperation(
                            accountType,
                            accountNumber,
                            currencyCode,
                            date,
                            transactionId,
                            description,
                            deposit,
                            withdraw,
                            card
                    );
                } catch (ParseException e) {
                    throw new IllegalArgumentException(e);
                }
            }).toList();

            return new AccountReport<>(reportId, operations);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static CardOperation parseCardOperationDescription(final String description) throws ParseException {
        final String regex = "([0-9]{6}[+0-9 ]{10,13}) (\\p{ASCII}{49}) (\\d{2}\\.\\d{2}\\.\\d{2}) (\\d{2}\\.\\d{2}\\.\\d{2}) ([ 0-9.]{13}) ([A-Z]{3}) (.*?)(MCC\\d{4})";
        final Pattern p = Pattern.compile(regex);
        final Matcher matcher = p.matcher(description);

        CardOperation result = null;

        if (matcher.matches()) {
            final String cardNumber = matcher.group(1).trim();
            final String pos = matcher.group(2).trim();
            final String authDate = matcher.group(3).trim();
            final String purchaseDate = matcher.group(4).trim();
            final String amount = matcher.group(5).trim();
            final String currency = matcher.group(6).trim();
            final String extra = StringUtils.trimToNull(matcher.group(7).trim());
            final String mcc = matcher.group(8).trim().substring(3);

            final PosInfo posInfo = parsePosInfo(pos);

            final DecimalFormat decimals = createDecimalFormat(Locale.US);

            result = new CardOperation(
                    cardNumber,
                    posInfo,
                    LocalDate.parse(authDate, DATE_FORMAT),
                    LocalDate.parse(purchaseDate, DATE_FORMAT),
                    decimals.parse(amount).doubleValue(),
                    currency,
                    extra,
                    mcc
            );
        }

        return result;
    }

    static PosInfo parsePosInfo(final String posInfo) {
        final String[] parts = posInfo.split("[\\\\/]");

        final String posNumber = parts[0].trim();
        final String countryCode = parts[1].trim();
        final String city = parts.length < 4 ? null : parts[2].trim();
        final String street = parts.length < 5 ? null : parts[3].trim();
        final String merchant = parts[parts.length - 1].trim();

        return new PosInfo(posNumber, countryCode, city, street, merchant);
    }

    private static DecimalFormat createDecimalFormat(Locale locale) {
        return new DecimalFormat("###.##", DecimalFormatSymbols.getInstance(locale));
    }

    @Override
    public List<AccountReportId> findAll() {
        final FileFilter filter = pathname -> pathname.getName().endsWith(REPORT_EXT);

        return Arrays.stream(Objects.requireNonNull(this.getBaseDirectory().listFiles(filter)))
                .map(MAPPER)
                .toList();

    }

    @Override
    public List<AccountReportId> findUnprocessed() {
        final FileFilter filter = pathname -> pathname.getName().toLowerCase().endsWith(REPORT_EXT)
                && !pathname.getName().toLowerCase().endsWith(REPORT_PROCESSED_EXT);

        return Arrays.stream(Objects.requireNonNull(this.getBaseDirectory().listFiles(filter)))
                .map(MAPPER)
                .toList();
    }

    @Override
    public void markProcessed(AccountReportId reportId) {
        Validate.notNull(reportId);

        final File reportFile = new File(this.getBaseDirectory(), reportId.name());

        final String baseName = FilenameUtils.removeExtension(reportId.name());

        final File processedReportFile = new File(this.getBaseDirectory(), baseName + REPORT_PROCESSED_EXT);

        Validate.isTrue(reportFile.exists() && reportFile.canRead() && reportFile.canWrite());
        Validate.isTrue(!processedReportFile.exists());

        try {
            FileUtils.moveFile(reportFile, processedReportFile);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to mark processed", e);
        }
    }

    @Override
    public AccountReportId save(String name, InputStream content) throws IOException {
        Validate.notEmpty(name);
        Validate.notNull(content);
        Validate.isTrue(name.endsWith(REPORT_EXT), "Invalid name of report: ", name);
        Validate.isTrue(!name.endsWith(REPORT_PROCESSED_EXT), "Saving already processed reports is not allowed:", name);

        final File reportFile = new File(this.getBaseDirectory(), name);
        Validate.isTrue(!reportFile.exists(), "Report file already exists:", name);

        final String baseName = FilenameUtils.removeExtension(name);
        final File processedReportFile = new File(this.getBaseDirectory(), baseName + REPORT_PROCESSED_EXT);
        Validate.isTrue(!processedReportFile.exists(), "Report file with this name is already processed earlier:", name);

        FileUtils.copyInputStreamToFile(content, reportFile);

        return MAPPER.apply(reportFile);
    }
}
