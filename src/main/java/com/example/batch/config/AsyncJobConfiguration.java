package com.example.batch.config;

import com.example.batch.domain.Transaction3;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import javax.sql.DataSource;

@Configuration
public class AsyncJobConfiguration {
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Bean
    @StepScope
    public FlatFileItemReader<Transaction3> fileTransactionReader(
            @Value("#{stepExecutionContext['file']}") Resource resource
    ) {
//        Resource resource = new ClassPathResource("input/bigtransactions.csv");

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
    @StepScope
    public MultiResourcePartitioner partitioner(
            @Value("#{jobParameters['inputFiles']}") Resource[] resources
    ) {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();

        partitioner.setKeyName("file");
        partitioner.setResources(resources);

        return partitioner;
    }

    @Bean
    public TaskExecutorPartitionHandler partitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();

        partitionHandler.setStep(step1());
        partitionHandler.setTaskExecutor(new SimpleAsyncTaskExecutor());

        return partitionHandler;
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
                .reader(fileTransactionReader(null))
                .writer(writer(null))
                .build();
    }

    @Bean
    public Step partitionedMaster() {
        return this.stepBuilderFactory.get("step1")
                .partitioner(step1().getName(), partitioner(null))
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    public Job partitionedJob() {
        return this.jobBuilderFactory.get("partitionedJob")
                .start(partitionedMaster())
                .build();
    }
}
