package com.atithi.aavas.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class AppConfig implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        loadEnv();
    }

    private void loadEnv() {
        try {
            if (Files.exists(Paths.get(".env"))) {
                List<String> lines = Files.readAllLines(Paths.get(".env"));
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int delimiterIdx = line.indexOf('=');
                    if (delimiterIdx > 0) {
                        String key = line.substring(0, delimiterIdx).trim();
                        String value = line.substring(delimiterIdx + 1).trim();
                        // Remove optional surrounding quotes
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        // Only set if not already present in environment
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load .env file: " + e.getMessage());
        }
    }
}
