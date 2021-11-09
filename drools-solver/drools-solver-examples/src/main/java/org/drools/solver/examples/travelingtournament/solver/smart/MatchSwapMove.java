package org.drools.solver.examples.travelingtournament.solver.smart;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.drools.FactHandle;
import org.drools.WorkingMemory;
import org.drools.solver.core.localsearch.decider.accepter.tabu.TabuPropertyEnabled;
import org.drools.solver.core.move.Move;
import org.drools.solver.examples.travelingtournament.domain.Day;
import org.drools.solver.examples.travelingtournament.domain.Match;

/**
 * @author Geoffrey De Smet
 */
public class MatchSwapMove implements Move, TabuPropertyEnabled {

    private Match firstMatch;
    private Match secondMatch;

    public MatchSwapMove(Match firstMatch, Match secondMatch) {
        this.firstMatch = firstMatch;
        this.secondMatch = secondMatch;
    }

    public boolean isMoveDoable(WorkingMemory workingMemory) {
        return true;
    }

    public Move createUndoMove(WorkingMemory workingMemory) {
        return this;
    }

    public void doMove(WorkingMemory workingMemory) {
        FactHandle firstMatchHandle = workingMemory.getFactHandle(firstMatch);
        FactHandle secondMatchHandle = workingMemory.getFactHandle(secondMatch);
        Day oldFirstMatchDay = firstMatch.getDay();
        firstMatch.setDay(secondMatch.getDay());
        secondMatch.setDay(oldFirstMatchDay);
        workingMemory.update(firstMatchHandle, firstMatch);
        workingMemory.update(secondMatchHandle, secondMatch);
    }

    public List<? extends Object> getTabuPropertyList() {
        return Arrays.asList(firstMatch, secondMatch);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof MatchSwapMove) {
            MatchSwapMove other = (MatchSwapMove) o;
            return new EqualsBuilder()
                    .append(firstMatch, other.firstMatch)
                    .append(secondMatch, other.secondMatch)
                    .isEquals();
        } else {
            return false;
        }
    }

    public int hashCode() {
        return new HashCodeBuilder()
                .append(firstMatch)
                .append(secondMatch)
                .toHashCode();
    }

    public String toString() {
        return firstMatch + " <=> " + secondMatch;
    }

}
