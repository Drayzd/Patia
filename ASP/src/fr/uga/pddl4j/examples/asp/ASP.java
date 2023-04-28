package fr.uga.pddl4j.examples.asp;

import fr.uga.pddl4j.heuristics.state.StateHeuristic;
import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.parser.RequireKey;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.plan.SequentialPlan;
import fr.uga.pddl4j.planners.AbstractPlanner;
import fr.uga.pddl4j.planners.Planner;
import fr.uga.pddl4j.planners.PlannerConfiguration;
import fr.uga.pddl4j.planners.ProblemNotSupportedException;
import fr.uga.pddl4j.planners.SearchStrategy;
import fr.uga.pddl4j.planners.statespace.HSP;
import fr.uga.pddl4j.planners.statespace.search.StateSpaceSearch;
import fr.uga.pddl4j.problem.DefaultProblem;
import fr.uga.pddl4j.problem.Fluent;
import fr.uga.pddl4j.problem.Goal;
import fr.uga.pddl4j.problem.InitialState;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.State;
import fr.uga.pddl4j.problem.operator.Action;
import fr.uga.pddl4j.problem.operator.ConditionalEffect;
import fr.uga.pddl4j.util.BitVector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sat4j.core.VecInt;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * The class is an example. It shows how to create a simple A* search planner able to
 * solve an ADL problem by choosing the heuristic to used and its weight.
 *
 * @author D. Pellier
 * @version 4.0 - 30.11.2021
 */
@CommandLine.Command(name = "MyPlanner",
        version = "MyPlanner 1.0",
        description = "Solves a specified planning problem using a custom search strategy with SAT4J.",
        sortOptions = false,
        mixinStandardHelpOptions = true,
        headerHeading = "Usage:%n",
        synopsisHeading = "%n",
        descriptionHeading = "%nDescription:%n%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n")
public class ASP extends AbstractPlanner {


    private static final Logger LOGGER = LogManager.getLogger(ASP.class.getName());

    @Override
    public Problem instantiate(DefaultParsedProblem problem) {
        final Problem pb = new DefaultProblem(problem);
        pb.instantiate();
        return pb;
    }


    @Override
    public Plan solve(final Problem problem) {
        Plan plan = new SequentialPlan();

        List<Fluent> problemFluents = problem.getFluents();
        int nFluents = problemFluents.size();

        InitialState initialState = problem.getInitialState();
        BitVector initPosFluents = initialState.getPositiveFluents();

        int[] initFluentsSign = new int[nFluents];
        for (int i = 0; i < initFluentsSign.length; i++) {
            initFluentsSign[i] = -(i + 1);
        }

        int[] initPosFluentsArr = initPosFluents.stream().toArray();
        for (int i = 0; i < initPosFluentsArr.length; i++) {
            initFluentsSign[initPosFluentsArr[i]] = -initFluentsSign[initPosFluentsArr[i]];
        }

        Goal goal = (Goal) problem.getGoal();
        BitVector goalPositiveFluents = goal.getPositiveFluents();

        int[] goalFluentsSign = new int[nFluents];
        for (int i = 0; i < goalFluentsSign.length; i++) {
            goalFluentsSign[i] = -(i + 1);
        }

        int[] goalPosFluents = goalPositiveFluents.stream().toArray();
        for (int i = 0; i < goalPosFluents.length; i++) {
            goalFluentsSign[goalPosFluents[i]] = -goalFluentsSign[goalPosFluents[i]];
        }

        List<Action> problemActions = problem.getActions();

        int[][][] actionArr = new int[problemActions.size()][3][];
        int[][] mid = new int[3][];

        int nAction = 0;

        for (Action problemAction : problemActions) {
            mid[0] = problemAction.getPrecondition().getPositiveFluents().stream().toArray();
            mid[1] = problemAction.getConditionalEffects().get(0).getEffect().getPositiveFluents().stream().toArray();
            mid[2] = problemAction.getConditionalEffects().get(0).getEffect().getNegativeFluents().stream().toArray();

            for (int j = 0; j < mid[0].length; j++) {
                mid[0][j] = mid[0][j] + 1;
            }
            for (int j = 0; j < mid[1].length; j++) {
                mid[1][j] = mid[1][j] + 1;
            }

            for (int j = 0; j < mid[2].length; j++) {
                mid[2][j] = -(mid[2][j] + 1);
            }

            actionArr[nAction][0] = mid[0];
            actionArr[nAction][1] = mid[1];
            actionArr[nAction][2] = mid[2];

            nAction++;
        }

        
        int m = 0;
        for (Fluent flu : problemFluents) {
            if (m < flu.getArguments().length) {
                m = flu.getArguments().length;
            }
        }

        final int NUM_CLAUSES = nAction;

        ISolver solver = SolverFactory.newDefault();

        try {
            solver.addClause(new VecInt(initFluentsSign));
            solver.addClause(new VecInt(goalFluentsSign));
        } catch (ContradictionException e) {
            throw new RuntimeException(e);
        }

        List<Integer> clauseMid = new ArrayList<>();

        for (int i = 0; i < NUM_CLAUSES; i++) {
            for (int prec : actionArr[i][0]) {
                clauseMid.add(prec);
            }
            for (int pos : actionArr[i][1]) {
                clauseMid.add(-pos);
            }
            for (int neg : actionArr[i][2]) {
                clauseMid.add(-neg);
            }
            int[] secondmid = new int[clauseMid.size()];
            for (int j = 0; j < clauseMid.size(); j++) {
                secondmid[j] = clauseMid.get(j);
            }
            try {
                solver.addClause(new VecInt(secondmid));
            } catch (ContradictionException e) {
                throw new RuntimeException(e);
            }
            clauseMid.clear();
        }

        try {
            if (solver.isSatisfiable()) {
                System.out.println("sat");
                int[] model = solver.findModel();
                
                for (int i = 0; i < problemActions.size(); i++) {
                    boolean actionSatisfied = true;
                    for (int j : actionArr[i][0]) {
                        if (model[Math.abs(j) - 1] == (j > 0 ? 0 : 1)) {
                            actionSatisfied = false;
                            break;
                        }
                    }
                    if (actionSatisfied) {
                        for (int j : actionArr[i][1]) {
                            if (model[Math.abs(j) - 1] == (j > 0 ? 0 : 1)) {
                                actionSatisfied = false;
                                break;
                            }
                        }
                    }
                    if (actionSatisfied) {
                        for (int j : actionArr[i][2]) {
                            if (model[Math.abs(j) - 1] == (j > 0 ? 1 : 0)) {
                                actionSatisfied = false;
                                break;
                            }
                        }
                    }
                    if (actionSatisfied) {
                        plan.add(i, problemActions.get(i));
                    }
                }
                return plan;
            } else {
                System.out.println("unsat");
            }
        } catch (org.sat4j.specs.TimeoutException e) {
            e.printStackTrace();
        }

        return null;
        }

    /**
     * The main method of the <code>ASP</code> planner.
     *
     * @param args the arguments of the command line.
     */
    public static void main(String[] args) {
        try {
            final ASP planner = new ASP();
            

            CommandLine cmd = new CommandLine(planner);

            cmd.execute("resources/domain_blocks.pddl","resources/blocks_p001.pddl");

        } catch (IllegalArgumentException e) {
            LOGGER.fatal(e.getMessage());
        }

        try {
            final HSP planner = new HSP();
            

            CommandLine cmd = new CommandLine(planner);

            cmd.execute("resources/domain_blocks.pddl","resources/blocks_p001.pddl");

        } catch (IllegalArgumentException e) {
            LOGGER.fatal(e.getMessage());
        }
    }

    
    @Override
    public boolean isSupported(Problem arg0) {
        return true;
        //throw new UnsupportedOperationException("Unimplemented method 'isSupported'");
    }
}