package org.drools.spi;

import java.util.HashMap;
import java.util.Map;

import org.drools.StatefulSession;
import org.drools.util.ClassUtils;

/** 
 * Factory class that will return a RuleBaseUpdateListener based on the provided string className
 *
 */
public class RuleBaseUpdateListenerFactory {
    
    public static RuleBaseUpdateListener createListener(String className, StatefulSession session) {        
        try {
            RuleBaseUpdateListener listener = ( RuleBaseUpdateListener ) ClassUtils.instantiateObject( className );
            listener.setSession( session );
            
            return listener;            
        } catch ( Throwable e ) {
            throw new RuntimeException("Unable to instantiate RuleBaseUpdateListener '" + className + "'", e );
        }        
    }

}
