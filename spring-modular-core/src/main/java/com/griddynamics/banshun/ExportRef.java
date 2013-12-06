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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ExportRef implements BeanFactoryAware {

    /**
     * Name of the exported service. It's used to find this export reference by key
     * and also to find the actual service bean in injected bean factory.
     * <idref> is useful to inject the bean name to this field.
     */
    private final String target;

    /**
     * Constraint for this reference. The requested lookup calls should specify
     * the same interface.
     */
    private final Class<?> interfaceClass;

    private BeanFactory beanFactory;
}
