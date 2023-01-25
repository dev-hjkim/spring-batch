package com.example.batch.classifier;

import com.example.batch.domain.Customer2;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.classify.Classifier;

public class ZipCodeClassifier implements Classifier<Customer2, ItemProcessor<Customer2, Customer2>> {
    private ItemProcessor<Customer2, Customer2> oddItemProcessor;
    private ItemProcessor<Customer2, Customer2> evenItemProcessor;

    public ZipCodeClassifier(ItemProcessor<Customer2, Customer2> oddItemProcessor,
                             ItemProcessor<Customer2, Customer2> evenItemProcessor) {
        this.oddItemProcessor = oddItemProcessor;
        this.evenItemProcessor = evenItemProcessor;
    }

    @Override
    public ItemProcessor<Customer2, Customer2> classify(Customer2 classifiable) {
        if (Integer.parseInt(classifiable.getZip()) % 2 == 0) {
            return evenItemProcessor;
        } else {
            return oddItemProcessor;
        }
    }
}
