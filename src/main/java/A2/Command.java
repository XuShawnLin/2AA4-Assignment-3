package A2;
/**
 * Interface thats apart of the Command design principle holding execute and undo methods
 */
public interface Command {
    void execute();
    void undo();
}