package com.example.batch.itemprocessor;

import com.example.batch.domain.Customer2;
import org.springframework.batch.item.ItemProcessor;

public class EvenFilteringItemProcessor implements ItemProcessor<Customer2, Customer2> {
    @Override
    public Customer2 process(Customer2 item) {
        return Integer.parseInt(item.getZip()) % 2 == 0 ? null : item;
    }
}
