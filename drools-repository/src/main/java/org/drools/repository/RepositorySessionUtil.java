package org.drools.repository;

import java.io.File;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.log4j.Logger;

//import junit.framework.Assert;

/**
 * This is a utility to simulate session behavior for the test suite.
 * @author Michael Neale
 *
 */
public class RepositorySessionUtil {

    private static ThreadLocal repo = new ThreadLocal();
    
    private static final Logger log = Logger.getLogger( RepositorySessionUtil.class );

    public static boolean deleteDir(File dir) {
        
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    }    
    
    
    public static RulesRepository getRepository() throws RulesRepositoryException {
        Object repoInstance = repo.get();
        if ( repoInstance == null ) {
            
            File dir = new File( "repository" );
            log.debug( "DELETING test repo: " + dir.getAbsolutePath() );
            deleteDir( dir );
            log.debug( "TEST repo was deleted." );
            
            JCRRepositoryConfigurator config = new JackrabbitRepositoryConfigurator();
            
            //create a repo instance (startup)
            Repository repository = config.getJCRRepository(null);
            
            //create a session
            Session session;
            try {
                session = repository.login(new SimpleCredentials("alan_parsons", "password".toCharArray()));
                RulesRepositoryAdministrator admin = new RulesRepositoryAdministrator(session);
                //clear out and setup
                if (admin.isRepositoryInitialized()) {
                    admin.clearRulesRepository();
                }
                config.setupRulesRepository( session );
                
                repoInstance = new RulesRepository( session );
                repo.set( repoInstance );                
            } catch ( Exception e) {
                throw new RulesRepositoryException();
                //Assert.fail("Unable to initialise repository :" + e.getMessage());
            }
            

            
        }
        return (RulesRepository) repoInstance;        
    }

}
