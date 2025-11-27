package com.im.common.config;

import com.github.pagehelper.PageInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * MyBatis 配置类
 */
@Configuration
@MapperScan("com.im.**.mapper")
public class MybatisConfig {
    
    /**
     * SqlSessionFactory 配置
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        
        // 设置 Mapper XML 文件位置
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath*:mapper/**/*.xml"));
        
        // 设置类型别名包
        sessionFactory.setTypeAliasesPackage("com.im.**.entity");
        
        // 配置分页插件
        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("helperDialect", "mysql");
        properties.setProperty("reasonable", "true");
        properties.setProperty("supportMethodsArguments", "true");
        pageInterceptor.setProperties(properties);
        
        sessionFactory.setPlugins(pageInterceptor);
        
        return sessionFactory.getObject();
    }
}
