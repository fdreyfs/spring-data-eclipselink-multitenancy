package com.fdreyfs.tenancy.demo;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;

import org.eclipse.persistence.config.BatchWriting;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.fdreyfs.tenancy.eclipselink.EclipseLinkMultiTenantProvider;
import com.fdreyfs.tenancy.eclipselink.EclipseLinkTenantPerEMFJpaVendorAdapter;
import com.fdreyfs.tenancy.resolver.DefaultCurrentTenantResolver;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories
public class MultiTenancyConfiguration extends JpaBaseConfiguration {

    protected MultiTenancyConfiguration(DataSource dataSource, JpaProperties properties,
            ObjectProvider<JtaTransactionManager> jtaTransactionManagerProvider) {
        super(dataSource, properties, jtaTransactionManagerProvider);
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }
    
    @Bean(destroyMethod="destroy")
    public PersistenceProvider persistenceProvider() {
        EclipseLinkMultiTenantProvider eclipseLinkMultiTenantProvider = new EclipseLinkMultiTenantProvider();
        eclipseLinkMultiTenantProvider.setCurrentTenantResolver(new DefaultCurrentTenantResolver());
        return eclipseLinkMultiTenantProvider;
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        
        EclipseLinkTenantPerEMFJpaVendorAdapter eclipseLinkTenantPerEMFJpaVendorAdapter = new EclipseLinkTenantPerEMFJpaVendorAdapter();
        eclipseLinkTenantPerEMFJpaVendorAdapter.setPersistenceProvider(persistenceProvider());
        return eclipseLinkTenantPerEMFJpaVendorAdapter;
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
        final Map<String, Object> ret = new HashMap<>();
        ret.put(PersistenceUnitProperties.LOGGING_LEVEL, "FINE");
        ret.put(PersistenceUnitProperties.LOGGING_PARAMETERS, "true");
        ret.put(PersistenceUnitProperties.BATCH_WRITING, BatchWriting.JDBC);
        return ret;
    }
}
