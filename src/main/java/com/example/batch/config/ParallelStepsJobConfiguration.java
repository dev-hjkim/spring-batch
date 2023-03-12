package com.example.batch.config;

import com.example.batch.domain.Transaction3;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import javax.sql.DataSource;

@Configuration
public class ParallelStepsJobConfiguration {
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    public FlatFileItemReader<Transaction3> fileTransactionReader() {
        Resource resource = new ClassPathResource("input/bigtransactions.csv");

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
    public StaxEventItemReader<Transaction3> xmlTransactionReader() {
        Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
        unmarshaller.setClassesToBeBound(Transaction3.class);

        Resource resource = new ClassPathResource("input/bigtransactions.xml");

        return new StaxEventItemReaderBuilder<Transaction3>()
                .name("xmlFileTransactionReader")
                .resource(resource)
                .addFragmentRootElements("transaction")
                .unmarshaller(unmarshaller)
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
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
                .<Transaction3, Transaction3>chunk(100)
                .reader(fileTransactionReader())
                .writer(writer(null))
                .taskExecutor(new SimpleAsyncTaskExecutor())
                .build();
    }

    @Bean
    public Step step2() {
        return this.stepBuilderFactory.get("step2")
                .<Transaction3, Transaction3>chunk(100)
                .reader(xmlTransactionReader())
                .writer(writer(null))
                .build();
    }

    @Bean
    public Job parallelStepsJob() {
        Flow secondFlow = new FlowBuilder<Flow>("secondFlow")
                .start(step2())
                .build();

        Flow parallelFlow = new FlowBuilder<Flow>("parallelFlow")
                .start(step1())
                .split(new SimpleAsyncTaskExecutor())
                .add(secondFlow)
                .build();

        return this.jobBuilderFactory.get("parallelStepsJob")
                .start(parallelFlow)
                .end()
                .build();
    }
}
