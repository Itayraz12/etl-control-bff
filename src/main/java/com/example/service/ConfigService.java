package com.example.service;

import com.example.model.Transfer;
import com.example.model.Filter;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class ConfigService {

    public List<Transfer> getTransforers() {
        return Arrays.asList(
            new Transfer("tr-1", "Source A", "Destination B"),
            new Transfer("tr-2", "Source C", "Destination D")
        );
    }

    public List<Filter> getFilters() {
        return Arrays.asList(
            new Filter("f-1", "Filter 1", "rule1"),
            new Filter("f-2", "Filter 2", "rule2")
        );
    }
}
