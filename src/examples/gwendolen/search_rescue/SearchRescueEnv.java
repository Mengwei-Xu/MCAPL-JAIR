package gwendolen.search_rescue;

import ail.mas.DefaultEnvironment;
import ail.syntax.Action;
import ail.syntax.Predicate;
import ail.syntax.Term;
import ail.syntax.Unifier;
import ail.syntax.VarTerm;
import ail.util.AILConfig;
import ail.util.AILexception;
import ajpf.util.AJPFLogger;

public class SearchRescueEnv extends DefaultEnvironment {

    private static final String LOGNAME =
            "gwendolen.search_rescue.SearchRescueEnv";

    /*
     * Concrete environment configuration.
     *
     * These values are supplied by the .ail configuration file.
     *
     * humanPresent specifies whether the environment contains a human.
     * humanLocation specifies the hidden location of that human.
     * locationNumber specifies the number of locations in this
     * concrete search-and-rescue instance.
     */
    private boolean humanPresent;
    private String humanLocation;
    private int locationNumber;

    @Override
    public void configure(AILConfig configuration) {
        super.configure(configuration);

        if (configuration.containsKey("human.present")) {
            humanPresent = Boolean.parseBoolean(
                    (String) configuration.get("human.present")
            );
        } else {
            throw new IllegalArgumentException(
                    "Missing required configuration: human.present"
            );
        }

        if (configuration.containsKey("human.location")) {
            humanLocation =
                    ((String) configuration.get("human.location")).trim();
        } else {
            throw new IllegalArgumentException(
                    "Missing required configuration: human.location"
            );
        }

        if (configuration.containsKey("location.number")) {
            locationNumber = Integer.parseInt(
                    ((String) configuration.get("location.number")).trim()
            );
        } else {
            throw new IllegalArgumentException(
                    "Missing required configuration: location.number"
            );
        }

        AJPFLogger.info(
                LOGNAME,
                "Configured environment: humanPresent="
                        + humanPresent
                        + ", humanLocation="
                        + humanLocation
                        + ", locationNumber="
                        + locationNumber
        );
    }

    @Override
    public Unifier executeAction(String agName, Action act)
            throws AILexception {

        String functor = act.getFunctor();

        /*
         * fly_to(L)
         *
         * The drone may either:
         *
         *   (1) already be at some location L0, or
         *   (2) have no current at(...) percept, as in the initial move.
         *
         * If at(L0) exists, it is removed before at(L) is added.
         * If there is no current at(...) percept, there is simply
         * nothing to remove.
         *
         * Effect:
         *
         *     - at(L0), if one exists
         *     + at(L)
         */
        if (functor.equals("fly_to")) {
            Term location = locationArg(act, 0);
            String locationName = location.toString();

            removeOldLocation(agName);

            addPercept(
                    agName,
                    unaryPredicate("at", location)
            );

            AJPFLogger.info(
                    LOGNAME,
                    agName + " flew to " + locationName
            );
        }

        /*
         * observe(L)
         *
         * Event-B precondition:
         *
         *     not checked(L)
         *
         * The environment then determines which of the two possible
         * observation outcomes occurs.
         *
         * Empty location:
         *
         *     + checked(L)
         *     + empty(L)
         *     + resolved(L)
         *
         * Human location:
         *
         *     + checked(L)
         *     + human(L)
         *
         * A human location is not resolved until aid is delivered.
         */
        else if (functor.equals("observe")) {
            Term location = locationArg(act, 0);
            String locationName = location.toString();

            /*
             * Precondition:
             *
             *     not checked(L)
             */
            if (hasPercept(
                    agName,
                    "checked",
                    location)) {

                throw new AILexception(
                        "Precondition of observe("
                                + locationName
                                + ") violated: checked("
                                + locationName
                                + ") already holds"
                );
            }

            addPercept(
                    agName,
                    unaryPredicate("checked", location)
            );

            /*
             * The hidden physical environment determines whether
             * this is the human-location or empty-location outcome.
             */
            if (humanPresent
                    && humanLocation.equals(locationName)) {

                addPercept(
                        agName,
                        unaryPredicate("human", location)
                );

                /*
                 * Do not add resolved(L).
                 *
                 * The location becomes resolved only after
                 * deliver_aid(L).
                 */
                AJPFLogger.info(
                        LOGNAME,
                        agName + " observed a human at "
                                + locationName
                );

            } else {

                addPercept(
                        agName,
                        unaryPredicate("empty", location)
                );

                addPercept(
                        agName,
                        unaryPredicate("resolved", location)
                );

                AJPFLogger.info(
                        LOGNAME,
                        agName + " observed an empty location at "
                                + locationName
                );
            }
        }

        /*
         * deliver_aid(L)
         *
         * precondition:
         *
         *     human(L)
         *
         * Effect:
         *
         *     + aid_delivered(L)
         *     + resolved(L)
         *
         * The hidden Java variables humanPresent and humanLocation
         * determine whether human(L) is generated during observation.
         * Once human(L) has been perceived, human(L) itself is the
         * action precondition, matching the Event-B action description.
         */
        else if (functor.equals("deliver_aid")) {
            Term location = locationArg(act, 0);
            String locationName = location.toString();

            /*
             * Precondition:
             *
             *     human(L)
             */
            if (!hasPercept(
                    agName,
                    "human",
                    location)) {

                throw new AILexception(
                        "Precondition of deliver_aid("
                                + locationName
                                + ") violated: human("
                                + locationName
                                + ") does not hold"
                );
            }

            addPercept(
                    agName,
                    unaryPredicate("aid_delivered", location)
            );

            addPercept(
                    agName,
                    unaryPredicate("resolved", location)
            );

            AJPFLogger.info(
                    LOGNAME,
                    agName + " delivered aid at "
                            + locationName
            );
        }

        /*
         * complete_mission
         *
         * precondition:
         *
         *     every location is resolved
         *
         * For a concrete instance with locationNumber = n,
         * this means:
         *
         *     resolved(l1)
         *     ...
         *     resolved(ln)
         *
         * Effect:
         *
         *     + mission(complete)
         */
        else if (functor.equals("complete_mission")) {

            /*
             * Precondition:
             *
             *     all locations are resolved
             */
            if (!allLocationsResolved(agName)) {
                throw new AILexception(
                        "Precondition of complete_mission violated: "
                                + "not every location is resolved"
                );
            }

            Predicate missionComplete =
                    new Predicate("mission");

            missionComplete.addTerm(
                    new Predicate("complete")
            );

            addPercept(
                    agName,
                    missionComplete
            );

            AJPFLogger.info(
                    LOGNAME,
                    agName
                            + " completed the search-and-rescue mission"
            );
        }

        return super.executeAction(agName, act);
    }

    /*
     * Check whether an agent-specific percept currently holds.
     *
     * Examples:
     *
     *     hasPercept(agName, "checked", l1)
     *     hasPercept(agName, "human", l2)
     *     hasPercept(agName, "resolved", l3)
     *
     * The environment adds these percepts using
     * addPercept(agName, ...), so they are stored in agPercepts.
     */
    private boolean hasPercept(
            String agName,
            String name,
            Term location) {

        Predicate target =
                unaryPredicate(name, location);

        return agPercepts.get(agName) != null
                && agPercepts.get(agName).contains(target);
    }

    /*
     * Check the precondition of complete_mission.
     *
     * For a concrete instance containing n locations, all
     * resolved(l1), ..., resolved(ln) percepts must hold.
     */
    private boolean allLocationsResolved(
            String agName) {

        for (int i = 1; i <= locationNumber; i++) {

            Term location =
                    new Predicate("l" + i);

            if (!hasPercept(
                    agName,
                    "resolved",
                    location)) {

                return false;
            }
        }

        return true;
    }

    /*
     * Remove any existing at(L) percept before the drone moves.
     *
     * If no at(...) percept currently exists, as on the first
     * execution of fly_to(L), this simply removes nothing.
     */
    private void removeOldLocation(
            String agName) {

        Predicate oldLocation =
                new Predicate("at");

        oldLocation.addTerm(
                new VarTerm("L")
        );

        removeUnifiesPercept(
                agName,
                oldLocation
        );
    }

    /*
     * Return the location argument of an action,
     * such as l1 or l10.
     */
    private Term locationArg(
            Action act,
            int index) {

        Term location =
                act.getTerm(index);

        if (location == null) {
            throw new IllegalArgumentException(
                    "Missing location argument in action: "
                            + act
            );
        }

        return location;
    }

    /*
     * Construct a unary predicate such as:
     *
     *     at(l1)
     *     checked(l4)
     *     human(l10)
     *     aid_delivered(l10)
     *     resolved(l10)
     *
     * Cloning prevents the percept from sharing the same
     * mutable term object as the executed action.
     */
    private Predicate unaryPredicate(
            String name,
            Term location) {

        Predicate predicate =
                new Predicate(name);

        Term copiedLocation =
                (Term) location.clone();

        predicate.addTerm(
                copiedLocation
        );

        return predicate;
    }
}