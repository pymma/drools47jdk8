package org.jboss.seam.remoting.gwt;
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



import org.drools.repository.RulesRepositoryException;
import org.jboss.seam.annotations.remoting.WebRemote;

public abstract class MyServiceThingie {

    public String something;

    @WebRemote
    public abstract void doSomething(String x);

    public String notMe() {
        return "Not allowed as doesn't have WebRemote";
    }

    @WebRemote
    public String yeahYeah() {
        return "whee";
    }

    @WebRemote
    public String notLoggedIn() {
        throw new FooNotLoggedInException();
    }

    @WebRemote
    public String goNuts() {
        throw new RulesRepositoryException("woo");
    }


    static class FooNotLoggedInException extends RuntimeException {

    }

}