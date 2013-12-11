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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.StringUtils;

import java.util.*;

import static java.util.Collections.emptyList;

public class DependencySorter {

    private static final Logger log = LoggerFactory.getLogger(DependencySorter.class);
    private static final Logger logGraph = LoggerFactory.getLogger("spring.nested.dependencies.graph");

    private List<Location> conflictContextGroup = emptyList();
    private boolean prohibitCycles = true;

    private final List<Location> locations;


    public DependencySorter(String[] configLocations, Map<String, List<BeanReferenceInfo>> imports, Map<String, BeanReferenceInfo> exports) {
        this.locations = prepareLocations(configLocations, imports, exports);
    }


    public void setProhibitCycles(boolean prohibitCycles) {
        this.prohibitCycles = prohibitCycles;
    }

    public String[] getConflictContextGroup() {
        return collectLocationNames(conflictContextGroup);
    }

    public String[] sort() {
        return collectLocationNames(sortLocations());
    }


    private List<Location> prepareLocations(String[] configLocations, Map<String, List<BeanReferenceInfo>> imports, Map<String, BeanReferenceInfo> exports) {
        List<Location> locations = new LinkedList<>();
        Map<String, Location> locationsMap = new HashMap<>();

        for (String locationName : configLocations) {
            Location location = new Location(locationName, new HashSet<BeanReferenceInfo>(), new HashSet<BeanReferenceInfo>());
            locations.add(location);
            locationsMap.put(location.locationName, location);
        }
        Set<String> allImportedBeans = fillLocationImportVectors(locationsMap, imports);
        fillLocationExportVectors(locationsMap, exports, allImportedBeans);
        showContextGraphStructure(locationsMap, exports);

        return locations;
    }

    private void showContextGraphStructure(Map<String, Location> locationsMap, Map<String, BeanReferenceInfo> exports) {
        if (logGraph.isDebugEnabled()) {
            logGraph.debug("digraph G {");

            for (String locationName : locationsMap.keySet()) {
                Map<String, StringBuilder> exportingLocations = new HashMap<>();

                for (BeanReferenceInfo bean : locationsMap.get(locationName).importBeans) {
                    String exportingLocation = exports.get(bean.getServiceName()).getLocation();

                    if (!exportingLocations.containsKey(exportingLocation)) {
                        exportingLocations.put(exportingLocation, new StringBuilder());
                    }
                    exportingLocations.get(exportingLocation).append(bean.getServiceName()).append("\\n");
                }
                for (String exportingLocationName : exportingLocations.keySet()) {
                    String beans = exportingLocations.get(exportingLocationName).toString();
                    logGraph.debug("\"{}\" -> \"{}\" [label = \"{}\"];", new Object[]{StringUtils.getFilename(locationName),
                            StringUtils.getFilename(exportingLocationName), beans});
                }
            }
            logGraph.debug("}");
        }
    }

    private void fillLocationExportVectors(Map<String, Location> locations, Map<String, BeanReferenceInfo> beans, Set<String> allImportedBeans) {
        for (String beanName : beans.keySet()) {
            if (allImportedBeans.contains(beanName)) {
                Location location = locations.get(beans.get(beanName).getLocation());
                location.exportBeans.add(beans.get(beanName));
            }
        }
    }

    private Set<String> fillLocationImportVectors(Map<String, Location> locations, Map<String, List<BeanReferenceInfo>> importedBeans) {
        Set<String> allImportBeans = new HashSet<>();

        for (String beanName : importedBeans.keySet()) {
            List<BeanReferenceInfo> beans = importedBeans.get(beanName);

            for (BeanReferenceInfo bean : beans) {
                Location location = locations.get(bean.getLocation());
                location.importBeans.add(bean);
                allImportBeans.add(bean.getServiceName());
            }
        }
        return allImportBeans;
    }

    private List<Location> sortLocations() {
        Deque<Location> stack = new LinkedList<>(locations);

        List<Location> sorted = pullLocationListHead(stack);
        if (stack.isEmpty()) {
            return sorted;
        }

        List<Location> tail = pullLocationListTail(stack);

        if (!stack.isEmpty()) {
            String message = "Cyclic dependencies found in child contexts: ";
            if (prohibitCycles) {
                throw new BeanCreationException(message + stack);
            }
            log.warn(message + "{}", stack);
        }
        conflictContextGroup = new ArrayList<>(stack);

        sorted.addAll(stack); //add conflict locations
        sorted.addAll(tail);  //add rest

        return sorted;
    }

    private List<Location> pullLocationListHead(Deque<Location> locations) {
        Set<String> resolvedBeans = new LinkedHashSet<>();
        List<Location> resolvedLocations = new LinkedList<>();

        for (Iterator<Location> it = locations.iterator(); it.hasNext();) {
            Location location = it.next();

            if (resolvedBeans.containsAll(location.getImportBeanNames())) {
                resolvedBeans.addAll(location.getExportBeanNames());
                resolvedLocations.add(location);

                it.remove(); //remove location from unresolved
                it = locations.iterator(); //reset iterator
            }
        }
        return resolvedLocations;
    }

    private List<Location> pullLocationListTail(Deque<Location> locations) {
        LinkedList<Location> resolvedLocations = new LinkedList<>();
        List<String> annihilatedExports = new LinkedList<>();

        for (Iterator<Location> it = locations.descendingIterator(); it.hasNext();) {
            Location location = it.next();

            if (annihilatedExports.containsAll(location.getExportBeanNames())) {
                it.remove(); //remove location from unresolved
                resolvedLocations.addFirst(location);

                for (BeanReferenceInfo imp : location.importBeans) {
                    if (isSomewhereImported(locations, imp)) {
                        annihilatedExports.add(imp.getServiceName());
                    }
                }
                it = locations.descendingIterator(); //reset iterator
            }
        }
        return resolvedLocations;
    }

    private boolean isSomewhereImported(Collection<Location> locations, BeanReferenceInfo importedBean) {
        for (Location location : locations) {
            if (location.getImportBeanNames().contains(importedBean.getServiceName())) {
                return false;
            }
        }
        return true;
    }

    private String[] collectLocationNames(List<Location> locations) {
        List<String> result = new ArrayList<>(locations.size());

        for (Location location : locations) {
            result.add(location.locationName);
        }
        return result.toArray(new String[result.size()]);
    }


    static class Location {
        final String locationName;
        final Set<BeanReferenceInfo> importBeans;
        final Set<BeanReferenceInfo> exportBeans;

        Location(String locationName, Set<BeanReferenceInfo> importBeans, Set<BeanReferenceInfo> exportBeans) {
            this.locationName = locationName;
            this.importBeans = importBeans;
            this.exportBeans = exportBeans;
        }

        Set<String> getImportBeanNames() {
            return collectBeanNames(importBeans);
        }

        Set<String> getExportBeanNames() {
            return collectBeanNames(exportBeans);
        }

        private Set<String> collectBeanNames(Collection<BeanReferenceInfo> beanReferences) {
            Set<String> result = new LinkedHashSet<>();
            for (BeanReferenceInfo bean : beanReferences) {
                result.add(bean.getServiceName());
            }
            return result;
        }

        public String toString() {
            return locationName;
        }
    }
}
