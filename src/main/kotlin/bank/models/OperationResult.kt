package bank.models

data class OperationResult(
        val account: Account?,
        val violations: Array<Violation>,
        val transactions: List<Transaction>
)