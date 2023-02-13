package com.example.batch.config;

import com.example.batch.domain.Neo4jCustomer;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.data.Neo4jItemWriter;
import org.springframework.batch.item.data.builder.Neo4jItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@EnableBatchProcessing
@Configuration
public class FormattedTextFileJob {
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;

    public FormattedTextFileJob(JobBuilderFactory jobBuilderFactory,
                                StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    //////////////////////////// STEP 1 ////////////////////////////

    @Bean
    public FlatFileItemReader<Neo4jCustomer> customerFileReader() {
        Resource inputFile = new ClassPathResource("input/customer.csv");

        return new FlatFileItemReaderBuilder<Neo4jCustomer>()
                .name("customerFileReader")
                .delimited()
                .names(new String[] {"firstName",
                        "middleInitial",
                        "lastName",
                        "address",
                        "city",
                        "state",
                        "zip"})
                .targetType(Neo4jCustomer.class)
                .resource(inputFile)
                .build();
    }

    @Bean
    public Neo4jItemWriter<Neo4jCustomer> neo4jItemWriter(SessionFactory sessionFactory) {
        return new Neo4jItemWriterBuilder<Neo4jCustomer>()
                .sessionFactory(sessionFactory)
                .build();
    }

    @Bean
    public Step neo4jFormatStep() throws Exception {
        return this.stepBuilderFactory.get("neo4jFormatStep")
                .<Neo4jCustomer, Neo4jCustomer> chunk(10)
                .reader(customerFileReader())
                .writer(neo4jItemWriter(null))
                .build();
    }

    @Bean
    public Job neo4jFormatJob() throws Exception {
        return this.jobBuilderFactory.get("neo4jFormatJob")
                .start(neo4jFormatStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
