package com.example.batch.config;

import com.example.batch.domain.Transaction3;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;

import javax.sql.DataSource;

@Configuration
@Profile("!master")
@EnableBatchIntegration
public class WorkerConfiguration {
    private final RemotePartitioningWorkerStepBuilderFactory workerStepBuilderFactory;

    public WorkerConfiguration(RemotePartitioningWorkerStepBuilderFactory workerStepBuilderFactory) {
        this.workerStepBuilderFactory = workerStepBuilderFactory;
    }

    @Bean
    public DirectChannel requests() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow inboundFlow(ConnectionFactory connectionFactory) {
        return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, "requests"))
                .channel(requests())
                .get();
    }

    @Bean
    public DirectChannel replies() {
        return new DirectChannel();
    }

    public Step workerStep() {
        return this.workerStepBuilderFactory.get("workerStep")
                .inputChannel(requests())
                .outputChannel(replies())
                .<Transaction3, Transaction3> chunk(100)
                .reader(fileTransactionReader(null))
                .writer(writer(null))
                .build();
    }


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
    public JdbcBatchItemWriter<Transaction3> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction3>()
                .dataSource(dataSource)
                .sql("INSERT INTO TRANSACTION3 (ACCOUNT, AMOUNT, TIMESTAMP) " +
                        "VALUES (:account, :amount, :timestamp)")
                .beanMapped()
                .build();
    }
}
