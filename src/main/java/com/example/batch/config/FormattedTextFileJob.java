package com.example.batch.config;

import com.example.batch.domain.Customer2;
import com.example.batch.preparedstatementsetter.CustomerItemPreparedStatementSetter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;

@EnableBatchProcessing
@Configuration
public class FormattedTextFileJob {
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;
    private DataSource dataSource;

    public FormattedTextFileJob(JobBuilderFactory jobBuilderFactory,
                                StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    //////////////////////////// STEP 1 ////////////////////////////

    @Bean
    public FlatFileItemReader<Customer2> customerFileReader() {
        Resource inputFile = new ClassPathResource("input/customer.csv");

        return new FlatFileItemReaderBuilder<Customer2>()
                .name("customerFileReader")
                .delimited()
                .names(new String[] {"firstName",
                        "middleInitial",
                        "lastName",
                        "address",
                        "city",
                        "state",
                        "zip"})
                .targetType(Customer2.class)
                .resource(inputFile)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Customer2> jdbcCustomerWriter(DataSource dataSource) throws Exception {
        return new JdbcBatchItemWriterBuilder<Customer2>()
                .dataSource(dataSource)
                .sql("INSERT INTO Customer2 (first_name, " +
                        "middle_initial, " +
                        "last_name, " +
                        "address, " +
                        "city, " +
                        "state, " +
                        "zip) VALUES (?, ?, ?, ?, ?, ?, ?)")
                .itemPreparedStatementSetter(new CustomerItemPreparedStatementSetter())
                .build();
    }

    @Bean
    public Step xmlFormatStep() throws Exception {
        return this.stepBuilderFactory.get("xmlFormatStep")
                .<Customer2, Customer2> chunk(10)
                .reader(customerFileReader())
                .writer(jdbcCustomerWriter(dataSource))
                .build();
    }

    @Bean
    public Job xmlFormatJob() throws Exception {
        return this.jobBuilderFactory.get("xmlFormatJob")
                .start(xmlFormatStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
