package com.example.batch.config;

import com.example.batch.domain.Transaction3;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import javax.sql.DataSource;

@Configuration
public class MultithreadJobConfiguration {
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public FlatFileItemReader<Transaction3> fileTransactionReader() {
        Resource resource = new ClassPathResource("input/transactions.csv");

        return new FlatFileItemReaderBuilder<Transaction3>()
                .name("transactionItemReader")
                .resource(resource)
                .saveState(false)
                .delimited()
                .names(new String[] {"account", "amount", "timestamp"})
                .fieldSetMapper(fieldSet -> {
                    Transaction3 transaction = new Transaction3();

                    transaction.setAccount(fieldSet.readString("account"));
                    transaction.setAmount(fieldSet.readBigDecimal("amount"));
                    transaction.setTimestamp(fieldSet.readDate("timestamp", "yyyy-MM-dd HH:mm:ss"));

                    return transaction;
                })
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Transaction3> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction3>()
                .dataSource(dataSource)
                .sql("INSERT INTO TRANSACTION3 (ACCOUNT, AMOUNT, TIMESTAMP) " +
                        "VALUES (:account, :amount, :timestamp)")
                .beanMapped()
                .build();
    }

    @Bean
    public Job multithreadJob() {
        return this.jobBuilderFactory.get("multithreadJob")
                .start(step1())
                .build();
    }

    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
                .<Transaction3, Transaction3>chunk(100)
                .reader(fileTransactionReader())
                .writer(writer(null))
                .taskExecutor(new SimpleAsyncTaskExecutor())
                .build();
    }
}
