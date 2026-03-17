package A2;


/**
 * Dice represents the pair of six-sided dice used in the game.
 */
public class Dice {
    private final Die die1;
    private final Die die2;

    public Dice() {
        this.die1 = new Die();
        this.die2 = new Die();
    }

    public Dice(Die die1, Die die2) {
        this.die1 = (die1 == null) ? new Die() : die1;
        this.die2 = (die2 == null) ? new Die() : die2;
    }

    /** @return sum in {2..12} */
    public int roll() {
        return die1.roll() + die2.roll();
    }
}
