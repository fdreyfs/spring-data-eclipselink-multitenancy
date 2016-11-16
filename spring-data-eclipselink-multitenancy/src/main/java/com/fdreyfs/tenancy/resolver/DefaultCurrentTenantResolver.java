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

package com.fdreyfs.tenancy.resolver;

import com.fdreyfs.tenancy.TenantContextHolder;

/**
 * Default implementation of {@link CurrentTenantResolver} using a {@link ThreadLocal} mechanism.<br />
 * In this case the <code>tenantId</code> is of type {@link Long}
 * <p>
 * Usage : <blockquote>
 * <pre>
 *     {@literal @}Bean(destroyMethod="destroy")
 *     public PersistenceProvider persistenceProvider() {
 *         EclipseLinkMultiTenantProvider eclipseLinkMultiTenantProvider = new EclipseLinkMultiTenantProvider();
 *         eclipseLinkMultiTenantProvider.setCurrentTenantResolver(new DefaultCurrentTenantResolver());
 *         return eclipseLinkMultiTenantProvider;
 *     }
 * </pre>
 * </blockquote>
 * 
 * 
 * @author Frederic.Dreyfus
 *
 */
public class DefaultCurrentTenantResolver implements CurrentTenantResolver<Long> {

    @Override
    public Long getCurrentTenantId() {
        return TenantContextHolder.get();
    }

    @Override
    public Long nullSafeGetCurrentTenantId() {
        return TenantContextHolder.nullSafeGet();
    }

}
