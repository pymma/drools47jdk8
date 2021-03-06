package org.drools.common;

import org.drools.Agenda;
import org.drools.spi.Activation;
import org.drools.spi.AgendaFilter;
import org.drools.spi.AgendaGroup;
import org.drools.spi.ConsequenceException;

public interface InternalAgenda
    extends
    Agenda {

    public void fireActivation(final Activation activation) throws ConsequenceException;

    public void removeScheduleItem(final ScheduledAgendaItem item);
    
    public org.drools.util.LinkedList getScheduledActivationsLinkedList();

    public boolean fireNextItem(AgendaFilter filter) throws ConsequenceException;

    public void scheduleItem(final ScheduledAgendaItem item);

    public void addAgendaGroup(final AgendaGroup agendaGroup);

    public void increaseActiveActivations();

    public void decreaseActiveActivations();

    public void increaseDormantActivations();

    public void decreaseDormantActivations();

    public int getActiveActivations();

    public int getDormantActivations();

}
