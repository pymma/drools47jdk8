package org.drools.base.mvel;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.WorkingMemory;
import org.drools.common.DroolsObjectInputStream;
import org.drools.reteoo.ReteTuple;
import org.drools.rule.Declaration;
import org.drools.spi.KnowledgeHelper;
import org.drools.spi.Tuple;
import org.mvel.DataConversion;
import org.mvel.integration.VariableResolver;
import org.mvel.integration.impl.BaseVariableResolverFactory;
import org.mvel.integration.impl.LocalVariableResolverFactory;
import org.mvel.integration.impl.StaticMethodImportResolverFactory;

public class DroolsMVELFactory extends BaseVariableResolverFactory
    implements
    LocalVariableResolverFactory,
    Externalizable,
    Cloneable {

    private static final long serialVersionUID = 400L;

    /**
     * Holds the instance of the variables.
     */
    private Object[]          tupleObjects;

    private KnowledgeHelper   knowledgeHelper;

    private Object            object;

    private Map               localDeclarations;

    private Map               previousDeclarations;

    private Map               globals;

    private WorkingMemory     workingMemory;

    private Map               localVariables;

    static {
        //for handling dates as string literals
        DataConversion.addConversionHandler( Date.class,
                                             new MVELDateCoercion() );
        DataConversion.addConversionHandler( Calendar.class,
                                             new MVELCalendarCoercion() );
    }

    // used only by serialization
    public DroolsMVELFactory() {
    }

    public DroolsMVELFactory(final Map previousDeclarations,
                             final Map localDeclarations,
                             final Map globals) {
        this( previousDeclarations,
              localDeclarations,
              globals,
              null );
    }

    public DroolsMVELFactory(final Map previousDeclarations,
                             final Map localDeclarations,
                             final Map globals,
                             final List[] externals) {
        this.previousDeclarations = previousDeclarations;
        this.localDeclarations = localDeclarations;
        this.globals = globals;

        if ( externals != null && MVELDebugHandler.isDebugMode() ) {
            for ( int i = 0; i < externals.length; i++ ) {
                for ( Iterator it = externals[i].iterator(); it.hasNext(); ) {
                    String identifier = (String) it.next();
                    isResolveable( identifier );
                }
            }
        }
    }

    public void writeExternal(final ObjectOutput stream) throws IOException {
        stream.writeObject( this.previousDeclarations );
        stream.writeObject( this.localDeclarations );
        stream.writeObject( this.globals );
        stream.writeObject( this.localVariables );
        stream.writeObject( this.variableResolvers != null ? new HashSet( this.variableResolvers.keySet() ) : null );
    }

    public void readExternal(final ObjectInput stream) throws IOException,
                                                      ClassNotFoundException {
        DroolsObjectInputStream droolsInputStream = (DroolsObjectInputStream) stream;

        this.previousDeclarations = (Map) droolsInputStream.readObject();
        this.localDeclarations = (Map) droolsInputStream.readObject();
        this.globals = (Map) droolsInputStream.readObject();
        this.localVariables = (Map) droolsInputStream.readObject();

        // restore resolvers
        Set resolvers = (Set) droolsInputStream.readObject();
        if ( resolvers != null ) {
            for ( Iterator it = resolvers.iterator(); it.hasNext(); ) {
                String name = (String) it.next();
                if ( !isResolveable( name ) ) {
                    addResolver( name,
                                 new LocalVariableResolver( this,
                                                            name ) );
                }
            }
        }
    }

    public static void addStaticImport(StaticMethodImportResolverFactory factory,
                                       String staticImportEntry,
                                       ClassLoader classLoader) {
        int index = staticImportEntry.lastIndexOf( '.' );
        String className = staticImportEntry.substring( 0,
                                                        index );
        String methodName = staticImportEntry.substring( index + 1 );

        try {
            Class cls = classLoader.loadClass( className );
            Method[] methods = cls.getDeclaredMethods();
            for ( int i = 0; i < methods.length; i++ ) {
                if ( methods[i].getName().equals( methodName ) ) {
                    factory.createVariable( methodName,
                                            methods[i] );
                    break;
                }
            }
        } catch ( ClassNotFoundException e ) {
            throw new RuntimeException( "Unable to dynamically load method '" + staticImportEntry + "'" );
        }

    }

    public Map getVariableResolvers() {
        return this.variableResolvers;
    }

    public Object getObject() {
        return this.object;
    }

    public WorkingMemory getWorkingMemory() {
        return this.workingMemory;
    }

    public void setContext(final Tuple tuple,
                           final KnowledgeHelper knowledgeHelper,
                           final Object object,
                           final WorkingMemory workingMemory,
                           final Map variables) {
        if ( tuple != null ) {
            this.tupleObjects = ((ReteTuple) tuple).toObjectArray();
        }
        this.knowledgeHelper = knowledgeHelper;
        this.object = object;
        this.workingMemory = workingMemory;
        if ( variables == null ) {
            if ( this.localVariables == null ) {
                this.localVariables = new HashMap();
            } else {
                this.localVariables.clear();
            }
        } else {
            this.localVariables = variables;
        }
    }

    public KnowledgeHelper getKnowledgeHelper() {
        return this.knowledgeHelper;
    }

    public Object getValue(final Declaration declaration) {
        int i = declaration.getPattern().getOffset();
        return this.tupleObjects[i];
    }

    public Object getValue(final String identifier) {
        return this.workingMemory.getGlobal( identifier );
    }

    public Object getLocalValue(final String identifier) {
        return this.localVariables.get( identifier );
    }

    public void setLocalValue(final String identifier,
                              final Object value) {
        this.localVariables.put( identifier,
                                 value );
    }

    public VariableResolver createVariable(String name,
                                           Object value) {
        VariableResolver vr = getVariableResolver( name );
        if ( vr == null ) {
            addResolver( name,
                         vr = new LocalVariableResolver( this,
                                                         name ) );
        }

        vr.setValue( value );
        return vr;
    }

    public VariableResolver createVariable(String name,
                                           Object value,
                                           Class type) {
        VariableResolver vr = getVariableResolver( name );
        if ( vr == null ) {
            addResolver( name,
                         vr = new LocalVariableResolver( this,
                                                         name,
                                                         type ) );
        }

        vr.setValue( value );
        return vr;
    }

    public boolean isResolveable(String name) {
        if ( DroolsMVELKnowledgeHelper.DROOLS.equals( name ) ) {
            addResolver( DroolsMVELKnowledgeHelper.DROOLS,
                         new DroolsMVELKnowledgeHelper( this ) );
            return true;

        } else if ( this.variableResolvers != null && this.variableResolvers.containsKey( name ) ) {
            return true;
        } else if ( this.previousDeclarations != null && this.previousDeclarations.containsKey( name ) ) {
            addResolver( name,
                         new DroolsMVELPreviousDeclarationVariable( (Declaration) this.previousDeclarations.get( name ),
                                                                    this ) );
            return true;
        } else if ( this.localDeclarations != null && this.localDeclarations.containsKey( name ) ) {
            addResolver( name,
                         new DroolsMVELLocalDeclarationVariable( (Declaration) this.localDeclarations.get( name ),
                                                                 this ) );
            return true;
        } else if ( this.globals.containsKey( name ) ) {
            addResolver( name,
                         new DroolsMVELGlobalVariable( name,
                                                       (Class) this.globals.get( name ),
                                                       this ) );
            return true;
        } else if ( nextFactory != null ) {
            return nextFactory.isResolveable( name );
        }

        return false;
    }

    private void addResolver(String name,
                             VariableResolver vr) {
        if ( this.variableResolvers == null ) {
            this.variableResolvers = new HashMap();
        }
        this.variableResolvers.put( name,
                                    vr );
    }

    public boolean isTarget(String name) {
        if ( this.variableResolvers != null ) {
            return this.variableResolvers.containsKey( name );
        } else {
            return false;
        }
    }

    public Object clone() {
        return new DroolsMVELFactory( this.previousDeclarations,
                                      this.localDeclarations,
                                      this.globals );
    }
}
