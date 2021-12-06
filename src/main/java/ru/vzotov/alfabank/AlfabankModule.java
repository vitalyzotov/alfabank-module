package ru.vzotov.alfabank;

import ru.vzotov.alfabank.domain.model.AlfabankOperation;
import ru.vzotov.alfabank.infrastructure.fs.AlfabankReportRepositoryFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.vzotov.accounting.domain.model.AccountReportRepository;

@Configuration
public class AlfabankModule {

    private static final Logger log = LoggerFactory.getLogger(AlfabankModule.class);

    @Bean
    public AccountReportRepository<AlfabankOperation> accountReportRepositoryAlfabank(
            @Value("${alfabank.reports.path}") String baseDirectoryPath) {

        log.info("Create alfabank report repository for path {}", baseDirectoryPath);

        return new AlfabankReportRepositoryFiles(baseDirectoryPath);
    }


}
