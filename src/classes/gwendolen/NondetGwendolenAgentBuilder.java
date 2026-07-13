package gwendolen;

import ail.semantics.AILAgent;
import ail.syntax.ast.Abstract_Capability;
import ail.syntax.ast.Abstract_Goal;
import ail.syntax.ast.Abstract_Literal;
import ail.syntax.ast.Abstract_Plan;
import ail.syntax.ast.Abstract_Rule;
import ail.util.AILexception;
import gwendolen.semantics.GwendolenAgent;
import gwendolen.semantics.NondetGwendolenAgent;
import gwendolen.syntax.ast.Abstract_GwendolenAgent;

public class NondetGwendolenAgentBuilder extends GwendolenAgentBuilder {

    @Override
    public AILAgent getAgent(String filename) {
        parsefile(filename);

        if (abs_agent == null) {
            throw new RuntimeException("Could not parse Gwendolen file: " + filename);
        }

        try {
            NondetGwendolenAgent agent =
                    new NondetGwendolenAgent(abs_agent.getAgName());

            copyStructures(abs_agent, agent);

            return agent;
        } catch (AILexception e) {
            throw new RuntimeException(
                    "Could not build nondeterministic Gwendolen agent",
                    e
            );
        }
    }

    private void copyStructures(
            Abstract_GwendolenAgent abstractAgent,
            GwendolenAgent agent
    ) {
        if (abstractAgent.beliefs != null) {
            for (Abstract_Literal belief : abstractAgent.beliefs) {
                agent.addInitialBel(belief.toMCAPL());
            }
        }

        if (abstractAgent.rules != null) {
            for (Abstract_Rule rule : abstractAgent.rules) {
                agent.addRule(rule.toMCAPL());
            }
        }

        if (abstractAgent.plans != null) {
            for (Abstract_Plan plan : abstractAgent.plans) {
                try {
                    agent.addPlan(plan.toMCAPL());
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Could not add plan: " + plan,
                            e
                    );
                }
            }
        }

        if (abstractAgent.goals != null) {
            for (Abstract_Goal goal : abstractAgent.goals) {
                agent.addInitialGoal(goal.toMCAPL());
            }
        }

        if (abstractAgent.capabilities != null) {
            for (Abstract_Capability capability : abstractAgent.capabilities) {
                agent.addCapability(capability.toMCAPL());
            }
        }

        try {
            agent.initAg();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not initialise nondeterministic Gwendolen agent",
                    e
            );
        }
    }
}