package A3;
/**
  Interface that's incharge of the Strategy Design principal
  that evaluates rules and chooses the highest value action
 */
public interface Rule {
    double evaluate(Player p, Board board);
    boolean apply(Player p, Board board, BuildStructure buildService, int round);
}
