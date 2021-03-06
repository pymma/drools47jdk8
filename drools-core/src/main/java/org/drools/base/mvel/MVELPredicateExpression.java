package org.drools.base.mvel;

import java.io.Serializable;

import org.drools.WorkingMemory;
import org.drools.rule.Declaration;
import org.drools.spi.PredicateExpression;
import org.drools.spi.Tuple;
import org.mvel.MVEL;

public class MVELPredicateExpression
    implements
    PredicateExpression,
    Serializable  {
    private static final long       serialVersionUID = 400L;

    private final Serializable      expr;
    private final DroolsMVELFactory prototype;
    private final String            exprStr;

    public MVELPredicateExpression(final Serializable expr,
                                   final DroolsMVELFactory factory,
                                   final String exprStr ) {
        this.expr = expr;
        this.prototype = factory;
        this.exprStr = exprStr;
    }
    
    public Object createContext() {
        return this.prototype.clone();
    }

    public boolean evaluate(final Object object,
                            final Tuple tuple,
                            final Declaration[] previousDeclarations,
                            final Declaration[] requiredDeclarations,
                            final WorkingMemory workingMemory,
                            final Object context ) throws Exception {
        DroolsMVELFactory factory = (DroolsMVELFactory) context;
        factory.setContext( tuple,
                                 null,
                                 object,
                                 workingMemory,
                                 null );
        final Boolean result = (Boolean) MVEL.executeExpression( this.expr,
                                                                 object,
                                                                 factory );
        return result.booleanValue();
    }
    
    public String toString() {
        return "eval( "+exprStr+" )";
    }

}
