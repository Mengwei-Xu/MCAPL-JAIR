package gwendolen.semantics;

import ail.syntax.ApplicablePlan;
import ail.syntax.Intention;
import ail.util.AILexception;
import ajpf.util.AJPFLogger;
import ajpf.util.choice.Choice;
import ail.syntax.AILAnnotation;
import ail.syntax.Literal;
import ail.syntax.StringTerm;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Gwendolen agent with nondeterministic applicable-plan selection.
 *
 * Normal Gwendolen selects the first applicable plan instance returned by
 * the applicable-plan iterator. This class instead collects all applicable
 * plan instances and uses an AJPF Choice object to select one.
 *
 * Internal AIL continuation instances are still selected normally, but are
 * omitted from INFO logging. Plan-library instances have IDs greater than
 * zero in this MCAPL version; internal instances use ID zero.
 */
public class NondetGwendolenAgent extends GwendolenAgent {

    private static final String LOGNAME =
            NondetGwendolenAgent.class.getName();

    private static final String BELIEF_LOG =
            "gwendolen.semantics.BeliefTrace";

    public NondetGwendolenAgent(String name) throws AILexception {
        super(name);
    }

    @Override
    public ApplicablePlan choosePlan(
            Iterator<ApplicablePlan> aps,
            Intention intention
    ) {
        List<ApplicablePlan> candidates =
                new ArrayList<ApplicablePlan>();

        while (aps.hasNext()) {
            candidates.add(aps.next());
        }

        if (candidates.isEmpty()) {
            return null;
        }

        /*
         * Exactly one applicable instance.
         *
         * Select every singleton normally, but log it only if it originated
         * from the agent's plan library rather than AIL's internal machinery.
         */
        if (candidates.size() == 1) {
            ApplicablePlan selected = candidates.get(0);

            if (isPlanLibraryInstance(selected)) {
                AJPFLogger.info(
                        LOGNAME,
                        "Only one plan-library applicable plan instance available"
                );

                AJPFLogger.info(
                        LOGNAME,
                        "Selected plan instance: " + selected
                );
            }

            updatePlanUsage(selected);
            return selected;
        }

        int planLibraryCandidateCount =
                countPlanLibraryInstances(candidates);

        /*
         * Report genuine plan-library choice points.
         *
         * In the normal search-rescue execution, every candidate at these
         * choice points comes from the plan library.
         */
        if (planLibraryCandidateCount > 0) {
            if (planLibraryCandidateCount == candidates.size()) {
                AJPFLogger.info(
                        LOGNAME,
                        "Nondeterministic choice among "
                                + candidates.size()
                                + " plan-library applicable plan instances"
                );
            } else {
                /*
                 * This mixed case is unlikely in the current example, but the
                 * message remains accurate if MCAPL supplies both internal and
                 * plan-library instances in the same selection.
                 */
                AJPFLogger.info(
                        LOGNAME,
                        "Nondeterministic choice among "
                                + candidates.size()
                                + " applicable plan instances; "
                                + planLibraryCandidateCount
                                + " originate from the plan library"
                );
            }
        }

        /*
         * Preserve the original nondeterministic semantics over every
         * applicable instance. Internal candidates are not filtered out;
         * they are merely omitted from the INFO log.
         */
        Choice<ApplicablePlan> choice =
                new Choice<ApplicablePlan>(
                        getMAS().getController()
                );

        double probability = 1.0 / candidates.size();

        for (ApplicablePlan candidate : candidates) {
            choice.addChoice(probability, candidate);
        }

        ApplicablePlan selected = choice.get_choice();

        if (isPlanLibraryInstance(selected)) {
            AJPFLogger.info(
                    LOGNAME,
                    "Selected plan instance: " + selected
            );
        }

        updatePlanUsage(selected);
        return selected;
    }

    /**
     * Plans loaded into an AIL plan library are numbered from 1.
     * AIL-generated continuation and bookkeeping instances use ID 0.
     */
    private boolean isPlanLibraryInstance(ApplicablePlan plan) {
        return plan != null && plan.getID() > 0;
    }

    private int countPlanLibraryInstances(
            List<ApplicablePlan> candidates
    ) {
        int count = 0;

        for (ApplicablePlan candidate : candidates) {
            if (isPlanLibraryInstance(candidate)) {
                count++;
            }
        }

        return count;
    }


    @Override
    public boolean addBel(
            Literal belief,
            AILAnnotation source,
            StringTerm beliefBase
    ) {
        boolean added =
                super.addBel(belief, source, beliefBase);

        if (added) {
            AJPFLogger.info(
                    BELIEF_LOG,
                    getAgName()
                            + " added belief: "
                            + belief
            );
        }

        return added;
    }
}