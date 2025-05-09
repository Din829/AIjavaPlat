package com.ding.aiplatjava.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * MyBatis配置类
 * 用于配置MyBatis的基本设置和扫描Mapper接口
 * [注意] 暂时注释掉，让 Spring Boot 自动配置生效，以解决 LocalDateTime 映射问题。
 */
//@Configuration // 暂时注释掉 @Configuration
@MapperScan("com.ding.aiplatjava.mapper") // 保留 MapperScan，因为它可能在主类上没有配置
public class MyBatisConfig {

    /**
     * 配置SqlSessionFactory
     * SqlSessionFactory是MyBatis的核心类，负责创建SqlSession对象
     *
     * @param dataSource 数据源
     * @return SqlSessionFactory实例
     * @throws Exception 可能的异常
     */
    //@Bean // 暂时注释掉 @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 设置实体类的别名包，简化XML中的类名书写
        factoryBean.setTypeAliasesPackage("com.ding.aiplatjava.entity");

        // 设置Mapper XML文件的位置
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        factoryBean.setMapperLocations(resolver.getResources("classpath:mapper/*.xml"));

        return factoryBean.getObject();
    }

    /**
     * 配置事务管理器
     * 用于管理数据库事务的开启、提交和回滚
     *
     * @param dataSource 数据源
     * @return 事务管理器实例
     */
    //@Bean // 暂时注释掉 @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 配置SqlSessionTemplate
     * SqlSessionTemplate是SqlSession的实现类，线程安全，可以被多个DAO共享
     *
     * @param sqlSessionFactory SqlSessionFactory实例
     * @return SqlSessionTemplate实例
     */
    //@Bean // 暂时注释掉 @Bean
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}