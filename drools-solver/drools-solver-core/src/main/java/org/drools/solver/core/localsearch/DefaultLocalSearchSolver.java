package org.drools.solver.core.localsearch;

import java.util.Random;

import org.drools.solver.core.evaluation.EvaluationHandler;
import org.drools.solver.core.localsearch.bestsolution.BestSolutionRecaller;
import org.drools.solver.core.localsearch.decider.Decider;
import org.drools.solver.core.localsearch.finish.Finish;
import org.drools.solver.core.move.Move;
import org.drools.solver.core.solution.Solution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Geoffrey De Smet
 */
public class DefaultLocalSearchSolver implements LocalSearchSolver, LocalSearchSolverLifecycleListener {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected long randomSeed; // TODO refactor to AbstractSolver

    protected EvaluationHandler evaluationHandler; // TODO refactor to AbstractSolver
    protected BestSolutionRecaller bestSolutionRecaller;
    protected Finish finish;
    protected Decider decider;

    protected long startingSystemTimeMillis;
    protected Random random;
    protected int stepIndex;
    protected double stepScore;

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public EvaluationHandler getEvaluationHandler() {
        return evaluationHandler;
    }

    public void setEvaluationHandler(EvaluationHandler evaluationHandler) {
        this.evaluationHandler = evaluationHandler;
    }

    public void setBestSolutionRecaller(BestSolutionRecaller bestSolutionRecaller) {
        this.bestSolutionRecaller = bestSolutionRecaller;
        bestSolutionRecaller.setLocalSearchSolver(this);
    }

    public void setDecider(Decider decider) {
        this.decider = decider;
        decider.setLocalSearchSolver(this);
    }

    public void setFinish(Finish finish) {
        this.finish = finish;
        finish.setLocalSearchSolver(this);
    }


    public long getTimeMillisSpend() {
        long now = System.currentTimeMillis();
        return now - startingSystemTimeMillis;
    }

    public Random getRandom() {
        return random;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public double getStepScore() {
        return stepScore;
    }

    public void setStartingSolution(Solution solution) {
        evaluationHandler.setSolution(solution);
    }

    public Solution getCurrentSolution() {
        return evaluationHandler.getSolution();
    }

    public int getBestSolutionStepIndex() {
        return bestSolutionRecaller.getBestSolutionStepIndex();
    }

    public Solution getBestSolution() {
        return bestSolutionRecaller.getBestSolution();
    }

    public double getBestScore() {
        return bestSolutionRecaller.getBestScore();
    }

    public double calculateTimeGradient() {
        return finish.calculateTimeGradient();
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public void solve() {
        solvingStarted();
        while (!finish.isFinished()) {
            beforeDeciding();
            Move nextStep = decider.decideNextStep();
            if (nextStep == null) {
                logger.warn("No move accepted for step ({}). Finishing early.", getStepIndex());
                break;
            }
            logger.info("Step ({}), time spend ({}) doing next step ({}).",
                    new Object[]{getStepIndex(), getTimeMillisSpend(), nextStep});
            stepDecided(nextStep);
            nextStep.doMove(evaluationHandler.getStatefulSession());
            stepScore = evaluationHandler.fireAllRulesAndCalculateStepScore();
            stepIndex++;
            stepTaken();
        }
        solvingEnded();
    }

    public void solvingStarted() {
        logger.info("Solving with random seed ({}).", randomSeed);
        startingSystemTimeMillis = System.currentTimeMillis();
        random = new Random(randomSeed);
        stepIndex = 0;
        stepScore = evaluationHandler.fireAllRulesAndCalculateStepScore();
        bestSolutionRecaller.solvingStarted();
        finish.solvingStarted();
        decider.solvingStarted();
    }

    public void beforeDeciding() {
        bestSolutionRecaller.beforeDeciding();
        finish.beforeDeciding();
        decider.beforeDeciding();
    }

    public void stepDecided(Move step) {
        bestSolutionRecaller.stepDecided(step);
        finish.stepDecided(step);
        decider.stepDecided(step);
    }

    public void stepTaken() {
        bestSolutionRecaller.stepTaken();
        finish.stepTaken();
        decider.stepTaken();
    }

    public void solvingEnded() {
        bestSolutionRecaller.solvingEnded();
        finish.solvingEnded();
        decider.solvingEnded();
        evaluationHandler.setSolution(bestSolutionRecaller.getBestSolution());
        logger.info("Solved in {} steps and {} time millis spend.", stepIndex, getTimeMillisSpend());
    }

}
