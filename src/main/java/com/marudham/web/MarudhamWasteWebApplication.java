package com.marudham.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.marudham.web.model.CollectionPoint;
import com.marudham.web.repository.CollectionPointRepository;

@SpringBootApplication
public class MarudhamWasteWebApplication {
    public static void main(String[] args) { SpringApplication.run(MarudhamWasteWebApplication.class, args); }
    @Bean CommandLineRunner seed(CollectionPointRepository points) {
        return args -> {
            if (points.count() == 0) {
                points.save(new CollectionPoint("CP01", "Market Street", "Central", 180));
                points.save(new CollectionPoint("CP02", "Bus Stand", "North", 320));
                points.save(new CollectionPoint("CP03", "College Road", "East", 95));
                points.save(new CollectionPoint("CP04", "Lake View", "South", 410));
            }
        };
    }
}
