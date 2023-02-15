package com.example.batch.config;

import com.example.batch.domain.GemfireCustomer;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.SpELItemKeyMapper;
import org.springframework.batch.item.data.GemfireItemWriter;
import org.springframework.batch.item.data.builder.GemfireItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.LocalRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;

import java.util.List;

@EnableBatchProcessing
@Configuration
@PeerCacheApplication(name = "AccessingDataGemFireApplication", logLevel = "info")
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
    public FlatFileItemReader<GemfireCustomer> customerFileReader() {
        Resource inputFile = new ClassPathResource("input/customer.csv");

        return new FlatFileItemReaderBuilder<GemfireCustomer>()
                .name("customerFileReader")
                .delimited()
                .names(new String[] {"firstName",
                        "middleInitial",
                        "lastName",
                        "address",
                        "city",
                        "state",
                        "zip"})
                .targetType(GemfireCustomer.class)
                .resource(inputFile)
                .build();
    }

    @Bean
    public GemfireItemWriter<Long, GemfireCustomer> gemfireItemWriter(GemfireTemplate gemfireTemplate) {
        return new GemfireItemWriterBuilder<Long, GemfireCustomer>()
                .template(gemfireTemplate)
                .itemKeyMapper(new SpELItemKeyMapper<>(
                        "firstName + middleInitial + lastName"))
                .build();
    }

    @Bean
    public Step gemfireFormatStep() throws Exception {
        return this.stepBuilderFactory.get("gemfireFormatStep")
                .<GemfireCustomer, GemfireCustomer> chunk(10)
                .reader(customerFileReader())
                .writer(gemfireItemWriter(null))
                .build();
    }

    @Bean
    public Job gemfireFormatJob() throws Exception {
        return this.jobBuilderFactory.get("gemfireFormatJob")
                .start(gemfireFormatStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean(name = "gemfireCustomer")
    public Region<Long, GemfireCustomer> getGemfireCustomer(final GemFireCache cache) throws Exception {
        LocalRegionFactoryBean<Long, GemfireCustomer> customerRegion = new LocalRegionFactoryBean<>();
        customerRegion.setCache(cache);
        customerRegion.setName("gemfireCustomer");
        customerRegion.afterPropertiesSet();
        Region<Long, GemfireCustomer> object = customerRegion.getRegion();
        return object;
    }

    @Bean
    public GemfireTemplate gemfireTemplate() throws Exception {
        return new GemfireTemplate(getGemfireCustomer(null));
    }

    @Bean
    public CommandLineRunner validator(final GemfireTemplate gemfireTemplate) {
        return args -> {
            List<Object> customers = gemfireTemplate.find("select * from /gemfireCustomer").asList();

            for (Object customer : customers) {
                System.out.println(">> object: " + customer);
            }
        };
    }
}
