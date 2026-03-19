package A3;

public class BuildSettlementRule implements Rule {

    private BuildValidator validator;

    public BuildSettlementRule(BuildValidator validator) {
        this.validator = validator;
    }

    @Override
    public double evaluate(Player p, Board board) {
        for (Node n : board.getNodes()) {
            if (validator.canBuildSettlement(p, n, board, false)) {
                return 1.0; //value associated with earning a VP
            }
        }
        return 0;
    }

    @Override
    public boolean apply(Player p, Board board, BuildStructure buildService, int round) {
        for (Node n : board.getNodes()) {
            if (validator.canBuildSettlement(p, n, board, false)
                    && buildService.buildSettlement(p, n, board)) {
                return true;
            }
        }
        return false;
    }
}
