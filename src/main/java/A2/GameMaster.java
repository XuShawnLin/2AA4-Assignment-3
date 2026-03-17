package A2;

public class GameMaster {

    private Board board;
    private Player[] players;
    private BuildStructure buildStructure;
    private Bank bank;
    private int currentPlayerIndex;
    private Dice dice;

    public GameMaster() {
        this.buildStructure = new BuildStructure();
        this.dice = new Dice();
        this.currentPlayerIndex = 0;
    }

    public void startGame() {
        this.board = new Board();
        this.bank = new Bank();
    }

    public Board getBoard() { return board; }
    public BuildStructure getBuildService() { return buildStructure; }
    public Bank getBank() { return bank; }
    public Player getCurrentPlayer() { return players[currentPlayerIndex]; }
    public void setPlayers(Player[] players) { this.players = players; }

    public int rollDice() {
        return dice.roll();
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.length;
    }

    public boolean checkWin() {
        return getCurrentPlayer().getVictoryPoints() >= 10;
    }

    /** Distribute resources based on dice roll */
    public void distributeResources(int roll) {
        if (roll == 7) return; // Robber handled elsewhere
        for (HexTile tile : board.getTiles()) {
            if (tile.getTokenNumber() != null && tile.getTokenNumber() == roll) {
                tile.resourceProduction();
            }
        }
    }
}