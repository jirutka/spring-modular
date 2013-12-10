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
package com.griddynamics.banshun.config.xml;

import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.Resource;

import java.io.IOException;

public final class ParserUtils {

    public static final String DEFAULT_ROOT_FACTORY_NAME = "root";

    // XML attribute names
    public static final String
            ID_ATTR = "id",
            INTERFACE_ATTR = "interface",
            NAME_ATTR = "name",
            REF_ATTR = "ref",
            ROOT_ATTR = "root";

    public static final String EXPORT_BEAN_DEF_ATTR_NAME = "com.griddynamics.banshun.EXPORT";
    public static final String IMPORT_BEAN_DEF_ATTR_NAME = "com.griddynamics.banshun.IMPORT";


    public static Class<?> findClassByName(String className, String beanName, ParserContext parserContext) {
        String description = parserContext.getReaderContext().getResource().getDescription();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new CannotLoadBeanClassException(description, beanName, className, ex);
        }
    }

    public static String extractResourcePath(Resource resource) {
        try {
            return resource.getURI().toString();
        } catch (IOException ex) {
            return resource.getDescription();
        }
    }

    private ParserUtils() {}
}
