package A3;

public class SpendCardsRule implements Rule {

    @Override
    public double evaluate(Player p, Board board) {
        if (p.getTotalResources() > 5) {
            return 0.5; //value associated with spending cards
        }
        return 0;
    }

    @Override
    public boolean apply(Player p, Board board, BuildStructure buildService, int round) {
        // reuse your existing method
        return Demonstrator.trySpendDownToLimit(p, board,
                buildService.getValidator(), buildService, round, 5);
    }
}