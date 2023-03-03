package com.example.batch.config;

import com.example.batch.domain.AppCustomer;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@EnableBatchProcessing
@Configuration
public class SaveToDbStepConfiguration {
    private StepBuilderFactory stepBuilderFactory;

    public SaveToDbStepConfiguration(StepBuilderFactory stepBuilderFactory) {
        this.stepBuilderFactory = stepBuilderFactory;
    }

    //////////////////////////// STEP 1 ////////////////////////////

    @Bean
    public FlatFileItemReader<AppCustomer> rawFileItemReader() {
        Resource inputFile = new ClassPathResource("input/app_customer.csv");

        return new FlatFileItemReaderBuilder<AppCustomer>()
                .name("rawFileItemReader")
                .delimited()
                .names(new String[] {"firstName",
                        "middleName",
                        "lastName",
                        "address1",
                        "address2",
                        "city",
                        "state",
                        "postalCode",
                        "emailAddress",
                        "homePhone",
                        "cellPhone",
                        "workPhone",
                        "notificationPref"})
                .targetType(AppCustomer.class)
                .resource(inputFile)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<AppCustomer> jdbcItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<AppCustomer>()
                .namedParametersJdbcTemplate(new NamedParameterJdbcTemplate(dataSource))
                .sql("INSERT INTO APP_CUSTOMER (first_name, middle_name, last_name, " +
                        "address1, address2, city, state, postal_code," +
                        "email_address, home_phone, cell_phone, work_phone, notification_pref) " +
                        "VALUES(:firstName, :middleName, :lastName," +
                        ":address1, :address2, :city, :state, :postalCode," +
                        ":emailAddress, :homePhone, :cellPhone, :workPhone, :notificationPref)")
                .beanMapped()
                .build();
    }

    @Bean
    public Step saveStep() throws Exception {
        return this.stepBuilderFactory.get("saveStep")
                .<AppCustomer, AppCustomer>chunk(10)
                .reader(rawFileItemReader())
                .writer(jdbcItemWriter(null))
                .build();
    }
}
