package com.example.batch.config;

import com.example.batch.domain.Transaction3;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.partition.DeployerPartitionHandler;
import org.springframework.cloud.task.batch.partition.DeployerStepExecutionHandler;
import org.springframework.cloud.task.batch.partition.PassThroughCommandLineArgsProvider;
import org.springframework.cloud.task.batch.partition.SimpleEnvironmentVariablesProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class BatchConfiguration {
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ConfigurableApplicationContext context;

    @Bean
    @Profile("master")
    public DeployerPartitionHandler partitionHandler(TaskLauncher taskLauncher,
                                                     JobExplorer jobExplorer,
                                                     ApplicationContext context,
                                                     Environment environment) {
        Resource resource = context.getResource("file:///path-to-jar/batch-0.0.1-SNAPSHOT.jar");
        DeployerPartitionHandler partitionHandler = new DeployerPartitionHandler(
                taskLauncher,
                jobExplorer,
                resource,
                "step1"
        );

        List<String> commandLineArgs = new ArrayList<>(3);
        commandLineArgs.add("--spring.profiles.active=worker");
        commandLineArgs.add("--spring.cloud.task.initialize.enable=false");
        commandLineArgs.add("--spring.batch.initializer.enabled=false");
        commandLineArgs.add("--spring.datasource.initialize=false");

        partitionHandler.setCommandLineArgsProvider(
                new PassThroughCommandLineArgsProvider(commandLineArgs)
        );
        partitionHandler.setEnvironmentVariablesProvider(
                new SimpleEnvironmentVariablesProvider(environment)
        );
        partitionHandler.setMaxWorkers(3);
        partitionHandler.setApplicationName("PartitionedBatchJobTask");

        return partitionHandler;
    }

    @Bean
    @Profile("worker")
    public DeployerStepExecutionHandler stepExecutionHandler(JobExplorer jobExplorer) {
        return new DeployerStepExecutionHandler(this.context, jobExplorer, this.jobRepository);
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

    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
                .<Transaction3, Transaction3>chunk(100)
                .reader(fileTransactionReader(null))
                .writer(writer(null))
                .build();
    }

    @Bean
    public Step partitionedMaster(PartitionHandler partitionHandler) {
        return this.stepBuilderFactory.get("step1")
                .partitioner(step1().getName(), partitioner(null))
                .partitionHandler(partitionHandler)
                .build();
    }

    @Bean
    public Job partitionedJob(PartitionHandler partitionHandler) {
        return this.jobBuilderFactory.get("partitionedJob")
                .start(partitionedMaster(partitionHandler))
                .build();
    }
}
