package org.drools.brms.client.rpc;
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



import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * Creates instances of the repository service for the client code to use.
 * @author Michael Neale
 */
public class RepositoryServiceFactory {

    /**
     * Change this to switch between debug/mock mode (ie web front end only)
     * versus full RPC (which requires the back end be running in some form).
     * Can set it to DEBUG if you want to run it client side only.
     */
    public static boolean DEBUG = false;
    public static RepositoryServiceAsync SERVICE;
    
    public static RepositoryServiceAsync getService() {
        if (SERVICE == null) {
            loadService();
        } 
        return SERVICE;
            
    }
    
    private static void loadService() {
        if (DEBUG)
            SERVICE = getMockService();
        else 
            SERVICE = getRealService(); 
        
    }

    private static RepositoryServiceAsync getMockService() {
        //return new MockRepositoryServiceAsync();
        return null;
    }

    private static RepositoryServiceAsync getRealService() {
        // define the service you want to call        
        RepositoryServiceAsync svc =
            (RepositoryServiceAsync) GWT.create(RepositoryService.class);
        ServiceDefTarget endpoint = (ServiceDefTarget) svc;
        
        String endpointURL = GWT.getModuleBaseURL() + "jbrmsService";        
        
        endpoint.setServiceEntryPoint(endpointURL);
        return svc;
    }

    /**
     * Perform the login.
     */
    public static void login(String userName, String password, AsyncCallback cb) {
        SecurityServiceAsync svc = getSecurityService();
        svc.login( userName, password, cb );
    }

    public static SecurityServiceAsync getSecurityService() {
        SecurityServiceAsync svc =
            (SecurityServiceAsync) GWT.create(SecurityService.class);
        ServiceDefTarget endpoint = (ServiceDefTarget) svc;
        String endpointURL = GWT.getModuleBaseURL() + "jbrmsService";        
        endpoint.setServiceEntryPoint(endpointURL);
        return svc;
    }
    
}