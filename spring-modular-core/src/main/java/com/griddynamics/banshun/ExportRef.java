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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import static lombok.AccessLevel.PACKAGE;

@Data
@AllArgsConstructor(access=PACKAGE)
public class ExportRef implements BeanFactoryAware {

    /**
     * External name of the exported bean by which other contexts can import it.
     */
    private final String target;

    /**
     * Interface of the exported bean. The {@link ContextParentBean#lookup(String, Class) lookup()}
     * calls should specify the same.
     */
    private final Class<?> interfaceClass;

    /**
     * Name of the actual bean being exported from the injected bean factory. This is used in
     * {@link ExportTargetSource} to find the bean. It may or may not be the same as export name!
     */
    private final String localBeanName;

    /**
     * Bean factory of the child context from which the bean will be exported.
     * This is automatically injected by Spring (see {@link BeanFactoryAware}).
     */
    private BeanFactory beanFactory;

    /**
     * This constructor is called by Spring when instantiating Bean Definition
     * declared in {@link com.griddynamics.banshun.config.xml.ExportBeanDefinitionParser}.
     */
    ExportRef(String target, Class<?> interfaceClass, String localBeanName) {
        this.target = target;
        this.interfaceClass = interfaceClass;
        this.localBeanName = localBeanName;
    }
}
