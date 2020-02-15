package bank.authoriser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import bank.authoriser.business.Authoriser
import bank.authoriser.business.EventFrequencyAnalyser
import bank.models.Account
import bank.models.Operation
import bank.models.OperationResult
import bank.models.Transaction
import bank.models.Violation

val accounts: MutableMap<String, Pair<Account, List<Transaction>>> = mutableMapOf()

//Dependencies
val authoriser = Authoriser(EventFrequencyAnalyser())
val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

fun main() {

    generateSequence(::readLine)
            .map { mapper.readValue<Operation>(it) }
            .mapNotNull { handleOperation(it) }
            .forEach { operationResult ->
                if (operationResult.violations.isEmpty()) {
                    operationResult.account?.let {
                        accounts[it.id] = Pair(it, operationResult.transactions)
                    }
                }
                println(mapper.writeValueAsString(operationResult))
            }

}

private fun handleOperation(operation: Operation): OperationResult? {
    return if (operation.account != null) {
        accounts.get(operation.account.id)
                ?.let { OperationResult(it.first, arrayOf(Violation.ACCOUNT_ALREADY_INITIALIZED), it.second) }
                ?: initialiseAccount(operation.account, mapper)
    } else if (operation.transaction != null) {
        val accountId = operation.transaction.accountId
        accounts.get(accountId)
                ?.let { acc -> authoriser.transaction(acc.first, operation.transaction, acc.second) }
                ?: OperationResult(null, arrayOf(Violation.ACCOUNT_DOES_NOT_EXIST), emptyList())
    } else {
        null
    }
}

private fun initialiseAccount(account: Account, mapper: ObjectMapper): OperationResult {
    accounts[account.id] = Pair(account, emptyList())
    return OperationResult(account, emptyArray(), emptyList())
}
