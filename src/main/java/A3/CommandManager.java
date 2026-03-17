package A3;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to manage the history of commands so undo and redo can happen
 */
public class CommandManager {

    private final List<Command> undoHistory; //holds the undo and redo history as lists
    private final List<Command> redoHistory;

    /**
     * Constructor for CommandManager
     */
    public CommandManager() {
        this.undoHistory = new ArrayList<>();
        this.redoHistory = new ArrayList<>(); //initalize lists for undo and redo
    }

    /**
     * Completes execute command but adding to history and clearing redo
     */
    public void executeCommand(Command command) {
        command.execute(); //execute the command
        undoHistory.add(command); //add it to the undo history
        redoHistory.clear(); //redo is nothing now
    }

    /**
     * Validator for undo
     */
    public boolean canUndo() {
        return !undoHistory.isEmpty(); //only can undo if theres something in the list
    }

    /**
     * Validator for redo
     */
    public boolean canRedo() {
        return !redoHistory.isEmpty(); //only can redo is theres something in the list
    }

    /**
     * Undo command
     */
    public boolean undo() {
        if (canUndo() == false) {
            return false; //if you cant undo aka validator failed, return false
        }

        Command command = undoHistory.remove(undoHistory.size() - 1); //remove the top undo command (like a stack)
        command.undo(); //undo the command
        redoHistory.add(command); //add it to the redo history
        return true;
    }

    /**
     * Redo command
     */
    public boolean redo() {
        if (canRedo() == false) {
            return false; //if you cant redo aka validator failed, return false
        }

        Command command = redoHistory.remove(redoHistory.size() - 1); //remove the top redo command (like a stack)
        command.execute(); //redo the command aka execute again
        undoHistory.add(command); //add it to the undo history
        return true;
    }
}