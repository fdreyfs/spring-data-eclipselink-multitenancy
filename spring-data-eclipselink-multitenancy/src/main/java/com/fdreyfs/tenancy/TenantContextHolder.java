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

package com.fdreyfs.tenancy;

import com.fdreyfs.tenancy.resolver.CurrentTenantResolver;

/**
 * {@link ThreadLocal} holding tenant id as a Long.<br />
 * Can be used with a {@link CurrentTenantResolver}.
 * 
 * <p>
 * This will find all entities for tenant id 1L: <blockquote>
 * 
 * <pre>
 * {@code
 * TenantContextHolder.set(1L);
 * repository.findAll();
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * 
 * @author Frederic.Dreyfus
 *
 */
public class TenantContextHolder {

    private static final ThreadLocal<Long> contextHolder = new ThreadLocal<Long>();

    public static void set(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Cannot set a null tenant id");
        }
        contextHolder.set(tenantId);
    }

    public static void clear() {
        contextHolder.remove();
    }

    public static Long get() {
        Long tenantId = contextHolder.get();
        if (tenantId == null) {
            throw new NoTenantException();
        }
        return tenantId;
    }

    public static Long nullSafeGet() {
        return contextHolder.get();
    }

}
