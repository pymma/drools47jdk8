package org.drools.repository;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;

/**
 * This is just an experimental access manager for proof of concept. Don't
 * actually use it or you are insane !
 */
public class MyAccessManager
    implements
    AccessManager {

    private AMContext amContext;

    public boolean canAccess(String arg0) throws NoSuchWorkspaceException,
                                         RepositoryException {
        System.out.println("can access " + arg0);
        return true;
    }

    public void checkPermission(ItemId arg0,
                                int arg1) throws AccessDeniedException,
                                         ItemNotFoundException,
                                         RepositoryException {
        System.out.println("check permission: " + arg0);

    }

    public void close() throws Exception {
        // TODO Auto-generated method stub

    }

    public void init(AMContext arg0) throws AccessDeniedException,
                                    Exception {
        
        this.amContext = arg0;        

    }

    public boolean isGranted(ItemId arg0,
                             int arg1) throws ItemNotFoundException,
                                      RepositoryException {
        if (arg0.denotesNode()) {
            NodeId id = (NodeId) arg0;
            System.out.println(arg0);
            
//            try {
//            Node n = RepositorySession.getRepository().getSession().getNodeByUUID( id.getUUID().toString() );
//            System.out.println(n.getName());
//            } catch (Exception e) {
//                System.out.println(e);
//            }
        }
        //System.out.println("is granted: " + arg0);
        return true;
    }

}
