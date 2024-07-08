package com.bilanee.octopus.config;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class DataSourceConfig {

    @Bean
    public MilkyLogFilter milkyLogFilter() {
        return new MilkyLogFilter();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource(List<Filter> filters) {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.getProxyFilters().addAll(filters);
        return druidDataSource;
    }

}
