/*
 * Copyright 2012 Grid Dynamics Consulting Services, Inc.
 *      http://www.griddynamics.com
 *
 * Copyright 2013 Jakub Jirutka <jakub@jirutka.cz>.
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
package com.griddynamics.banshun;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.TargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;

import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

public class ExportTargetSource implements TargetSource {

    private static final Logger log = LoggerFactory.getLogger(ExportTargetSource.class);

    private final AtomicReference<Object> target = new AtomicReference<>();

    private final String beanName;
    private final Class<?> serviceInterface;
    private final BeanFactory beanFactory;


    public ExportTargetSource(ExportRef exportRef) {
        this.beanName = exportRef.getBeanName();
        this.serviceInterface = exportRef.getServiceInterface();
        this.beanFactory = exportRef.getBeanFactory();
    }


    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public String getBeanName() {
        return beanName;
    }

    public Class<?> getTargetClass() {
        return serviceInterface;
    }

    public boolean isStatic() {
        return true;
    }

    public void releaseTarget(Object target) {
    }

    public Object getTarget() throws BeansException {
        Object localTarget = target.get();

        if (localTarget == null) {
            // verify if declared service interface is compatible with the real bean type
            Class<?> beanClass = beanFactory.getType(beanName);
            if (!serviceInterface.isAssignableFrom(beanClass)) {
                throw new BeanNotOfRequiredTypeException(beanName, serviceInterface, beanClass);
            }

            if (target.compareAndSet(null, localTarget = beanFactory.getBean(beanName))) {
                return localTarget;

            } else {
                log.debug("Redundant initialization of ExportTargetSource for bean '{}' caused by" +
                         "concurrency has been detected.", beanName);
                return target.get();
            }
        }
        return localTarget;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, "target");
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "target");
    }
}
