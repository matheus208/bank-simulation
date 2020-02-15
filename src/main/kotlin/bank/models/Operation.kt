package bank.models

data class Operation(
        val account: Account?,
        val transaction: Transaction?
)