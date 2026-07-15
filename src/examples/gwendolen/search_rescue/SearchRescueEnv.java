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
     * The environment contains at most one hidden human.
     *
     * The agent does not initially know whether a human is present
     * or at which location the human is located.
     */
    private boolean humanPresent = true;
    private String humanLocation = "l10";

    @Override
    public void configure(AILConfig configuration) {
        super.configure(configuration);

        if (configuration.containsKey("human.present")) {
            humanPresent = Boolean.parseBoolean(
                    (String) configuration.get("human.present")
            );
        }

        if (configuration.containsKey("human.location")) {
            humanLocation =
                    ((String) configuration.get("human.location")).trim();
        }

        AJPFLogger.info(
                LOGNAME,
                "Configured hidden human: present="
                        + humanPresent
                        + ", location="
                        + humanLocation
        );
    }

    @Override
    public Unifier executeAction(String agName, Action act)
            throws AILexception {

        String functor = act.getFunctor();

        /*
         * fly_to(L)
         *
         * Remove the drone's previous location and add at(L).
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
         * Every observed location becomes checked.
         *
         * An empty location becomes resolved immediately.
         * A location containing a human remains unresolved until aid
         * has been delivered.
         */
        else if (functor.equals("observe")) {
            Term location = locationArg(act, 0);
            String locationName = location.toString();

            addPercept(
                    agName,
                    unaryPredicate("checked", location)
            );

            if (humanPresent
                    && humanLocation.equals(locationName)) {

                addPercept(
                        agName,
                        unaryPredicate("human", location)
                );

                /*
                 * Do not add resolved(location).
                 *
                 * A location containing a human becomes resolved only
                 * after deliver_aid(location).
                 */
                AJPFLogger.info(
                        LOGNAME,
                        agName + " observed a human at " + locationName
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
         * Aid may be delivered only at the hidden human's location.
         * Successful delivery resolves that location.
         */
        else if (functor.equals("deliver_aid")) {
            Term location = locationArg(act, 0);
            String locationName = location.toString();

            if (humanPresent
                    && humanLocation.equals(locationName)) {

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
                        agName + " delivered aid at " + locationName
                );
            } else {
                /*
                 * This branch should be unreachable because the Gwendolen
                 * plan requires human(L) before deliver_aid(L) is executed.
                 *
                 * No percept is added if an invalid aid delivery is attempted.
                 */
                AJPFLogger.warning(
                        LOGNAME,
                        agName + " attempted to deliver aid at "
                                + locationName
                                + ", but no human was present"
                );
            }
        }

        /*
         * complete_mission
         *
         * The corresponding Gwendolen plan ensures that every location
         * has been resolved before this action is selected.
         *
         * Executing the action adds the belief mission(complete).
         */
        else if (functor.equals("complete_mission")) {
            Predicate missionComplete = new Predicate("mission");
            missionComplete.addTerm(new Predicate("complete"));

            addPercept(
                    agName,
                    missionComplete
            );

            AJPFLogger.info(
                    LOGNAME,
                    agName + " completed the search-and-rescue mission"
            );
        }

        return super.executeAction(agName, act);
    }

    /*
     * Remove any existing at(L) percept before the drone moves.
     */
    private void removeOldLocation(String agName) {
        Predicate oldLocation = new Predicate("at");
        oldLocation.addTerm(new VarTerm("L"));

        removeUnifiesPercept(agName, oldLocation);
    }

    /*
     * Return a symbolic location argument such as l1 or l10.
     */
    private Term locationArg(Action act, int index) {
        Term location = act.getTerm(index);

        if (location == null) {
            throw new IllegalArgumentException(
                    "Missing location argument in action: " + act
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
     * Cloning prevents the percept from sharing the same mutable term
     * object as the executed action.
     */
    private Predicate unaryPredicate(String name, Term location) {
        Predicate predicate = new Predicate(name);

        Term copiedLocation = (Term) location.clone();
        predicate.addTerm(copiedLocation);

        return predicate;
    }
}