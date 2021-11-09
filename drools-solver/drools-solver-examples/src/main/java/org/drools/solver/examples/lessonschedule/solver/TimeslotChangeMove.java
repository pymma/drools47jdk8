package org.drools.solver.examples.lessonschedule.solver;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.drools.FactHandle;
import org.drools.WorkingMemory;
import org.drools.solver.core.localsearch.decider.accepter.tabu.TabuPropertyEnabled;
import org.drools.solver.core.move.Move;
import org.drools.solver.examples.lessonschedule.domain.Lesson;
import org.drools.solver.examples.lessonschedule.domain.Timeslot;

/**
 * @author Geoffrey De Smet
 */
public class TimeslotChangeMove implements Move, TabuPropertyEnabled {

    private Lesson lesson;
    private Timeslot toTimeslot;

    public TimeslotChangeMove(Lesson lesson, Timeslot toTimeslot) {
        this.lesson = lesson;
        this.toTimeslot = toTimeslot;
    }

    public boolean isMoveDoable(WorkingMemory workingMemory) {
        Timeslot fromTimeslot = lesson.getTimeslot();
        if (fromTimeslot == null) {
            return (toTimeslot != null);
        }
        return !fromTimeslot.equals(toTimeslot);
    }

    public Move createUndoMove(WorkingMemory workingMemory) {
        return new TimeslotChangeMove(lesson, lesson.getTimeslot());
    }

    public void doMove(WorkingMemory workingMemory) {
        FactHandle lessonHandle = workingMemory.getFactHandle(lesson);
        lesson.setTimeslot(toTimeslot);
        workingMemory.update(lessonHandle, lesson);
    }

    public List<? extends Object> getTabuPropertyList() {
        return Collections.singletonList(lesson);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof TimeslotChangeMove) {
            TimeslotChangeMove other = (TimeslotChangeMove) o;
            return new EqualsBuilder()
                    .append(lesson, other.lesson)
                    .append(toTimeslot, other.toTimeslot)
                    .isEquals();
        } else {
            return false;
        }
    }

    public int hashCode() {
        return new HashCodeBuilder()
                .append(lesson)
                .append(toTimeslot)
                .toHashCode();
    }

    public String toString() {
        return lesson + " => " + toTimeslot;
    }

}
