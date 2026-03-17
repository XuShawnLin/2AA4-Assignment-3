package A3;

/**
 * Checks win conditions (SRP).
 */
public class WinChecker {

    public boolean hasWinner(Player[] players, int winningVP) {
        if (players == null) return false;
        for (Player p : players) {
            if (p != null && p.getVictoryPoints() >= winningVP) return true;
        }
        return false;
    }
}
