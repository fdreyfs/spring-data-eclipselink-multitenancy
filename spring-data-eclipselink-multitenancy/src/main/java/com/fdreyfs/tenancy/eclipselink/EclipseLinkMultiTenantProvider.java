/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fdreyfs.tenancy.eclipselink;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.jpa.JpaCache;

import com.fdreyfs.tenancy.resolver.CurrentTenantResolver;

/**
 * A {@link PersistenceProvider} that creates dynamically a {@link EntityManagerFactory} for each tenant.
 * 
 * @author Frederic.Dreyfus
 * 
 */
public class EclipseLinkMultiTenantProvider implements PersistenceProvider {

    /**
     * The EclipseLink {@link org.eclipse.persistence.jpa.PersistenceProvider} delegate
     */
    private PersistenceProvider persistenceProvider = new org.eclipse.persistence.jpa.PersistenceProvider();

    private CurrentTenantResolver<? extends Serializable> currentTenantResolver;

    /**
     * There is one handler per {@link EntityManagerFactory} created by the {@link PersistenceProvider}.<br />
     * There should be only one in the list...
     */
    private List<TenantEmfDispatcherHandler> tenantEmfDispatcherHandlers = new ArrayList<>();

    public EclipseLinkMultiTenantProvider() {
    }

    @Override
    @SuppressWarnings("rawtypes")
    public EntityManagerFactory createEntityManagerFactory(String emName, Map properties) {
        throw new UnsupportedOperationException(
                "Use instead : #createContainerEntityManagerFactory(PersistenceUnitInfo, Map)");
    }

    /**
     * Returns a proxy of {@link EntityManagerFactory} that will dispatch to the current tenant
     * {@link EntityManagerFactory}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
        TenantEmfDispatcherHandler tenantEmfDispatcherHandler = new TenantEmfDispatcherHandler(info, properties);
        tenantEmfDispatcherHandlers.add(tenantEmfDispatcherHandler);
        return (EntityManagerFactory) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { EntityManagerFactory.class }, tenantEmfDispatcherHandler);
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return persistenceProvider.getProviderUtil();
    }

    public void destroy() {
        for (TenantEmfDispatcherHandler tenantEmfDispatcherHandler : tenantEmfDispatcherHandlers) {
            tenantEmfDispatcherHandler.destroy();
        }
        tenantEmfDispatcherHandlers.clear();
    }

    public void removeTenantEmf() {
        for (TenantEmfDispatcherHandler tenantEmfDispatcherHandler : tenantEmfDispatcherHandlers) {
            tenantEmfDispatcherHandler.removeTenantEmf();
        }
    }

    /**
     * Clear second level caches for all EMFs
     */
    public void clean2LCaches() {
        for (TenantEmfDispatcherHandler tenantEmfDispatcherHandler : tenantEmfDispatcherHandlers) {
            tenantEmfDispatcherHandler.clean2LCaches();
        }
    }

    public Cache getCurrentTenant2LCache() {
        for (TenantEmfDispatcherHandler tenantEmfDispatcherHandler : tenantEmfDispatcherHandlers) {
            Cache tenant2LCache = tenantEmfDispatcherHandler.get2LCache(currentTenantResolver.getCurrentTenantId());
            return tenant2LCache;
        }
        return null;
    }

    public void cleanCurrentTenant2LCache() {
        for (TenantEmfDispatcherHandler tenantEmfDispatcherHandler : tenantEmfDispatcherHandlers) {
            Cache tenant2LCache = tenantEmfDispatcherHandler.get2LCache(currentTenantResolver.getCurrentTenantId());
            if (tenant2LCache != null) {
                JpaCache jpaCache = tenant2LCache.unwrap(JpaCache.class);
                jpaCache.evictAll();
            }
        }
    }

    public void printCurrentTenant2LCache() {
        for (TenantEmfDispatcherHandler tenantEmfDispatcherHandler : tenantEmfDispatcherHandlers) {
            Cache tenant2LCache = tenantEmfDispatcherHandler.get2LCache(currentTenantResolver.getCurrentTenantId());
            if (tenant2LCache != null) {
                JpaCache jpaCache = tenant2LCache.unwrap(JpaCache.class);
                jpaCache.print();
            }
        }
    }

    public void printCurrentTenant2LCache(Class<?> entityClass) {
        for (TenantEmfDispatcherHandler tenantEmfDispatcherHandler : tenantEmfDispatcherHandlers) {
            Cache tenant2LCache = tenantEmfDispatcherHandler.get2LCache(currentTenantResolver.getCurrentTenantId());
            if (tenant2LCache != null) {
                JpaCache jpaCache = tenant2LCache.unwrap(JpaCache.class);
                jpaCache.print(entityClass);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private class TenantEmfDispatcherHandler implements InvocationHandler {

        private PersistenceUnitInfo originalPersistenceUnitInfo;

        private Map originalProperties;

        private Map<Serializable, EntityManagerFactory> entityManagerFactories = new HashMap<>();

        public TenantEmfDispatcherHandler(PersistenceUnitInfo originalPersistenceUnitInfo, Map originalProperties) {
            this.originalPersistenceUnitInfo = originalPersistenceUnitInfo;
            this.originalProperties = originalProperties;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            EntityManagerFactory entityManagerFactory = getEntityManagerFactoryForCurrentTenant();
            return method.invoke(entityManagerFactory, args);
        }

        private EntityManagerFactory getEntityManagerFactoryForCurrentTenant() {
            Serializable tenantId = currentTenantResolver.nullSafeGetCurrentTenantId();
            EntityManagerFactory emf = entityManagerFactories.get(tenantId);
            if (emf == null) {
                emf = createTenantEntityManagerFactory(tenantId);
            }
            return emf;
        }

        @SuppressWarnings("unchecked")
        private synchronized EntityManagerFactory createTenantEntityManagerFactory(Serializable tenantId) {
            // double check
            EntityManagerFactory emf = entityManagerFactories.get(tenantId);
            if (emf != null) {
                return emf;
            }

            Map propertiesForEmf = new HashMap<>(originalProperties);
            PersistenceUnitInfo persistenceUnitInfoForEmf = null;

            String sessionName;
            if (tenantId != null) {
                sessionName = "tenant[" + tenantId + "]-session";
            } else {
                sessionName = "no-tenant-session";
            }
            propertiesForEmf.put(PersistenceUnitProperties.SESSION_NAME, sessionName);
            propertiesForEmf.put(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, tenantId);
            persistenceUnitInfoForEmf = (PersistenceUnitInfo) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] { PersistenceUnitInfo.class }, new TenantPersistenceUnitInfo(
                            originalPersistenceUnitInfo, tenantId));

            emf = persistenceProvider.createContainerEntityManagerFactory(persistenceUnitInfoForEmf, propertiesForEmf);
            entityManagerFactories.put(tenantId, emf);
            return emf;
        }

        public void destroy() {
            for (EntityManagerFactory emf : entityManagerFactories.values()) {
                if (emf.isOpen()) {
                    emf.close();
                }
            }
        }

        public void removeTenantEmf() {
            for (Map.Entry<Serializable, EntityManagerFactory> emfEntry : entityManagerFactories.entrySet()) {
                if (emfEntry.getKey() != null) {
                    emfEntry.getValue().close();
                }
            }
        }

        public void clean2LCaches() {
            for (EntityManagerFactory emf : entityManagerFactories.values()) {
                if (emf.isOpen()) {
                    emf.getCache().evictAll();
                }
            }
        }

        public Cache get2LCache(Serializable tenantId) {
            EntityManagerFactory emf = entityManagerFactories.get(tenantId);
            if (emf != null && emf.isOpen()) {
                return emf.getCache();
            }
            return null;
        }
    }

    /**
     * A {@link PersistenceUnitInfo} proxy that returns the correct puName for the tenant.
     * 
     * @author Frederic.Dreyfus
     * 
     */
    private static class TenantPersistenceUnitInfo implements InvocationHandler {

        private Serializable tenantId;
        private String persistenceUnitName;
        private PersistenceUnitInfo originalPersistenceUnitInfo;

        public TenantPersistenceUnitInfo(PersistenceUnitInfo puInfo, Serializable tenantId) {
            this.tenantId = tenantId;
            if (tenantId == null) {
                this.persistenceUnitName = "no-tenant";
            } else {
                this.persistenceUnitName = "tenant[" + tenantId + "]";
            }
            this.originalPersistenceUnitInfo = puInfo;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getPersistenceUnitName".equals(method.getName())) {
                return persistenceUnitName;
            } else {
                return method.invoke(originalPersistenceUnitInfo, args);
            }
        }

        @SuppressWarnings("unused")
        public Serializable getTenantId() {
            return tenantId;
        }

    }

    public CurrentTenantResolver<? extends Serializable> getCurrentTenantResolver() {
        return currentTenantResolver;
    }

    public void setCurrentTenantResolver(CurrentTenantResolver<? extends Serializable> currentTenantResolver) {
        this.currentTenantResolver = currentTenantResolver;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void generateSchema(PersistenceUnitInfo info, Map map) {
        persistenceProvider.generateSchema(info, map);

    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean generateSchema(String persistenceUnitName, Map map) {
        return persistenceProvider.generateSchema(persistenceUnitName, map);
    }

}
