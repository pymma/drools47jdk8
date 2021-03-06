package org.drools.compiler;

/*
 * Copyright 2005 JBoss Inc
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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.drools.RuntimeDroolsException;
import org.drools.base.accumulators.AccumulateFunction;
import org.drools.util.ChainedProperties;

/**
 * This class configures the package compiler. 
 * Dialects and their DialectConfigurations  are handled by the DialectRegistry
 * Normally you will not need to look at this class, unless you want to override the defaults.
 * 
 * This class is not thread safe and it also contains state. Once it is created and used 
 * in one or more PackageBuilders it should be considered immutable. Do not modify its 
 * properties while it is being used by a PackageBuilder.
 * 
 * drools.dialect.default = <String>
 * drools.accumulate.function.<function name> = <qualified class>
 * drools.dump.dir = <String>
 * 
 * default dialect is java.
 * Available preconfigured Accumulate functions are:
 * drools.accumulate.function.average = org.drools.base.accumulators.AverageAccumulateFunction
 * drools.accumulate.function.max = org.drools.base.accumulators.MaxAccumulateFunction
 * drools.accumulate.function.min = org.drools.base.accumulators.MinAccumulateFunction
 * drools.accumulate.function.count = org.drools.base.accumulators.CountAccumulateFunction
 * drools.accumulate.function.sum = org.drools.base.accumulators.SumAccumulateFunction 
 */
public class PackageBuilderConfiguration {

    private static final String ACCUMULATE_FUNCTION_PREFIX = "drools.accumulate.function.";

    private Map                 dialectConfigurations;

    private String              defaultDialect;

    private ClassLoader         classLoader;

    private ChainedProperties   chainedProperties;

    private Map                 accumulateFunctions;

    private File                dumpDirectory;

    /**
     * Constructor that sets the parent class loader for the package being built/compiled
     * @param classLoader
     */
    public PackageBuilderConfiguration(ClassLoader classLoader) {
        init( classLoader,
              null );
    }

    /**
     * Programmatic properties file, added with lease precedence
     * @param properties
     */
    public PackageBuilderConfiguration(Properties properties) {
        init( null,
              properties );
    }

    /**
     * Programmatic properties file, added with lease precedence
     * @param classLoader
     * @param properties
     */
    public PackageBuilderConfiguration(ClassLoader classLoader,
                                       Properties properties) {
        init( classLoader,
              properties );
    }

    public PackageBuilderConfiguration() {
        init( null,
              null );
    }

    private void init(ClassLoader classLoader,
                      Properties properties) {
        if ( classLoader == null ) {
            classLoader = Thread.currentThread().getContextClassLoader();
            if ( classLoader == null ) {
                classLoader = this.getClass().getClassLoader();
            }
        }
        setClassLoader( classLoader );

        this.chainedProperties = new ChainedProperties( this.classLoader,
                                                        "packagebuilder.conf" );

        if ( properties != null ) {
            this.chainedProperties.addProperties( properties );
        }

        this.dialectConfigurations = new HashMap();

        buildDialectConfigurationMap();

        buildAccumulateFunctionsMap();

        buildDumpDirectory();
    }

    public ChainedProperties getChainedProperties() {
        return this.chainedProperties;
    }

    private void buildDialectConfigurationMap() {
        //DialectRegistry registry = new DialectRegistry();
        Map dialectProperties = new HashMap();
        this.chainedProperties.mapStartsWith( dialectProperties,
                                              "drools.dialect",
                                              false );
        setDefaultDialect( (String) dialectProperties.remove( "drools.dialect.default" ) );

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for ( Iterator it = dialectProperties.entrySet().iterator(); it.hasNext(); ) {
            Entry entry = (Entry) it.next();
            String str = (String) entry.getKey();
            String dialectName = str.substring( str.lastIndexOf( "." ) + 1 );
            String dialectClass = (String) entry.getValue();
            addDialect( dialectName,
                        dialectClass );
        }
    }

    public void addDialect(String dialectName,
                           String dialectClass) {
        try {
            Class cls = classLoader.loadClass( dialectClass );
            DialectConfiguration dialectConf = (DialectConfiguration) cls.newInstance();
            dialectConf.init( this );
            addDialect( dialectName,
                        dialectConf );
        } catch ( Exception e ) {
            throw new RuntimeDroolsException( "Unable to load dialect '" + dialectClass + ":" + dialectName + "'",
                                              e );
        }
    }

    public void addDialect(String dialectName,
                           DialectConfiguration dialectConf) {
        dialectConfigurations.put( dialectName,
                                   dialectConf );
    }

    public DialectRegistry buildDialectRegistry() {
        DialectRegistry registry = new DialectRegistry();
        for ( Iterator it = this.dialectConfigurations.values().iterator(); it.hasNext(); ) {
            DialectConfiguration conf = (DialectConfiguration) it.next();
            Dialect dialect = conf.getDialect();
            registry.addDialect( conf.getDialect().getId(),
                                 dialect );
        }
        return registry;
    }

    public String getDefaultDialect() {
        return this.defaultDialect;
    }

    public void setDefaultDialect(String defaultDialect) {
        this.defaultDialect = defaultDialect;
    }

    public DialectConfiguration getDialectConfiguration(String name) {
        return (DialectConfiguration) this.dialectConfigurations.get( name );
    }

    public void setDialectConfiguration(String name,
                                        DialectConfiguration configuration) {
        this.dialectConfigurations.put( name,
                                        configuration );
    }

    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /** Use this to override the classloader that will be used for the rules. */
    public void setClassLoader(final ClassLoader classLoader) {
        if ( classLoader != null ) {
            this.classLoader = classLoader;
        }
    }

    private void buildAccumulateFunctionsMap() {
        this.accumulateFunctions = new HashMap();
        Map temp = new HashMap();
        this.chainedProperties.mapStartsWith( temp,
                                              ACCUMULATE_FUNCTION_PREFIX,
                                              true );
        for ( Iterator it = temp.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            String identifier = ((String) entry.getKey()).trim().substring( ACCUMULATE_FUNCTION_PREFIX.length() );
            this.accumulateFunctions.put( identifier,
                                          entry.getValue() );
        }
    }

    public Map getAccumulateFunctionsMap() {
        return Collections.unmodifiableMap( this.accumulateFunctions );
    }

    public void addAccumulateFunction(String identifier,
                                      String className) {
        this.accumulateFunctions.put( identifier,
                                      className );
    }

    public void addAccumulateFunction(String identifier,
                                      Class clazz) {
        this.accumulateFunctions.put( identifier,
                                      clazz.getName() );
    }

    public AccumulateFunction getAccumulateFunction(String identifier) {
        String className = (String) this.accumulateFunctions.get( identifier );
        if ( className == null ) {
            throw new RuntimeDroolsException( "No accumulator function found for identifier: " + identifier );
        }
        try {
            Class clazz = this.classLoader.loadClass( className );
            return (AccumulateFunction) clazz.newInstance();
        } catch ( ClassNotFoundException e ) {
            throw new RuntimeDroolsException( "Error loading accumulator function for identifier " + identifier + ". Class " + className + " not found",
                                              e );
        } catch ( InstantiationException e ) {
            throw new RuntimeDroolsException( "Error loading accumulator function for identifier " + identifier + ". Class " + className + " not found",
                                              e );
        } catch ( IllegalAccessException e ) {
            throw new RuntimeDroolsException( "Error loading accumulator function for identifier " + identifier + ". Class " + className + " not found",
                                              e );
        }
    }

    private void buildDumpDirectory() {
        String dumpStr = this.chainedProperties.getProperty( "drools.dump.dir",
                                                             null );
        if ( dumpStr != null ) {
            this.dumpDirectory = new File( dumpStr );
            if ( !dumpDirectory.isDirectory() || !dumpDirectory.canWrite() || !dumpDirectory.canRead() ) {
                this.dumpDirectory = null;
                throw new RuntimeDroolsException( "Drools dump directory is not accessible: " + dumpStr );
            }
        }
    }

    public File getDumpDir() {
        return this.dumpDirectory;
    }

    public void setDumpDir(File dumpDir) {
        if ( !dumpDir.isDirectory() || !dumpDir.canWrite() || !dumpDir.canRead() ) {
            throw new RuntimeDroolsException( "Drools dump directory is not accessible: " + dumpDir.toString() );
        }
        this.dumpDirectory = dumpDir;
    }

}