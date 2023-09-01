package com.bilanee.octopus.infrastructure;

import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import com.stellariver.milky.spring.partner.SectionLoader;
import com.stellariver.milky.spring.partner.UniqueIdBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author houchuang
 */
@Configuration
public class IdGetterConfiguration {


    @Bean
    public SectionLoader sectionLoader(DataSource dataSource) {
        return new SectionLoader(dataSource);
    }


    @Bean
    public UniqueIdBuilder uniqueIdBuilder(SectionLoader sectionLoader) {
        return new UniqueIdBuilder("unique_id", "default", sectionLoader);
    }

    @Bean
    public UniqueIdGetter uniqueIdGetter(UniqueIdBuilder uniqueIdBuilder) {
        return new UniqueIdGetterImpl(uniqueIdBuilder);
    }


    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class UniqueIdGetterImpl implements UniqueIdGetter{

        final UniqueIdBuilder uniqueIdBuilder;

        @Override
        public Long get() {
            return uniqueIdBuilder.get();
        }
    }

}
