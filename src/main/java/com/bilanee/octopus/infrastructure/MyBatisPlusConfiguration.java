package com.bilanee.octopus.infrastructure;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.ReplacePlaceholderInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.stellariver.milky.common.tool.stable.MilkyStableSupport;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * @author houchuang
 */

@SuppressWarnings("AliDeprecation")
@Configuration
@MapperScan(basePackages = "com.bilanee.octopus.infrastructure.mapper")
public class MyBatisPlusConfiguration {

    @Bean
    public SqlSessionFactory sqlSessionFactory(
            DataSource dataSource, @Autowired(required = false) MilkyStableSupport milkyStableSupport) throws Exception {

        MybatisSqlSessionFactoryBean sqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);

        sqlSessionFactoryBean.setTypeAliasesPackage("com.bilanee.octopus.infrastructure.entity");
        sqlSessionFactoryBean.setTypeHandlersPackage("com.bilanee.octopus.infrastructure.handlers");
        //驼峰转化开启
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setReturnInstanceForEmptyRow(true);
        sqlSessionFactoryBean.setConfiguration(configuration);

        //mapper
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] mapperResources = resolver.getResources("mapper/*.xml");
        sqlSessionFactoryBean.setMapperLocations(mapperResources);

        // 自定义日期自动更新器
        GlobalConfig globalConfig = GlobalConfigUtils.defaults();
        globalConfig.setMetaObjectHandler(new MyMetaObjectHandler());
        sqlSessionFactoryBean.setGlobalConfig(globalConfig);

        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        mybatisPlusInterceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        mybatisPlusInterceptor.addInnerInterceptor(new ReplacePlaceholderInnerInterceptor());
        sqlSessionFactoryBean.setPlugins(mybatisPlusInterceptor);

        return sqlSessionFactoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory baseSqlSessionFactory) {
        return new SqlSessionTemplate(baseSqlSessionFactory);
    }

}
