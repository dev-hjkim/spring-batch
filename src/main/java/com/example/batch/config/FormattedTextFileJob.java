package com.example.batch.config;

import com.example.batch.domain.Customer2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.xstream.XStreamMarshaller;

import java.util.HashMap;
import java.util.Map;

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
    public StaxEventItemWriter<Customer2> xmlCustomerWriter() {
        Resource outputFile = new FileSystemResource("output/xmlCustomer.xml");

        Map<String, Class> aliases = new HashMap<>();
        aliases.put("custmer", Customer2.class);

        XStreamMarshaller marshaller = new XStreamMarshaller();
        marshaller.setAliases(aliases);
        marshaller.setAutodetectAnnotations(true);
        marshaller.afterPropertiesSet();

        return new StaxEventItemWriterBuilder<Customer2>()
                .name("customerItemWriter")
                .resource(outputFile)
                .marshaller(marshaller)
                .rootTagName("customers")
                .build();
    }

    @Bean
    public Step xmlFormatStep() {
        return this.stepBuilderFactory.get("xmlFormatStep")
                .<Customer2, Customer2> chunk(10)
                .reader(customerFileReader())
                .writer(xmlCustomerWriter())
                .build();
    }

    @Bean
    public Job xmlFormatJob() {
        return this.jobBuilderFactory.get("xmlFormatJob")
                .start(xmlFormatStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
