package bank.authoriser.business

import bank.models.Account
import bank.models.OperationResult
import bank.models.Transaction
import bank.models.Violation
import java.time.Instant
import java.util.UUID

const val MAX_FREQUENCY_FOR_TIME_FRAME = 3
const val TIME_FRAME_IN_MINS_FOR_FREQUENCY = 2

const val MAX_SIMILAR_TRANSACTION_FOR_TIME_FRAME = 2
const val TIME_FRAME_IN_MINS_FOR_SIMILAR_TRANSACTION = 2

class Authoriser(private val frequencyAnalyser: EventFrequencyAnalyser) {

    /*
        Analyzes a new transaction, given an account
     */
    fun transaction(acc: Account, transaction: Transaction, transactionHistory: List<Transaction>): OperationResult =
            // Checks rules
            listOfNotNull(checkCard(acc),
                        checkLimit(acc, transaction, transactionHistory),
                        checkDoubled(transaction, transactionHistory),
                        checkFrequency(transaction, transactionHistory))
                    //Calculate the new balance. and show any violations
                    .let {
                        val updatedTransactionHistory = transactionHistory.plus(transaction)
                        OperationResult(
                                account = acc.copy(availableLimit = acc.availableLimit - updatedTransactionHistory.sumBy(Transaction::amount)),
                                violations = it.toTypedArray(),
                                transactions = updatedTransactionHistory)
                    }

    /*
    Active card check
    Checks if the account's card is active
    */
    private fun checkCard(acc: Account): Violation? =
            when {
                !acc.activeCard -> Violation.CARD_NOT_ACTIVE
                else -> null
            }

    /*
    Account limit check
    The sum of the transactions amount needs to be smaller than or equal to the available limit
    */
    private fun checkLimit(acc: Account, transaction: Transaction, transactions: List<Transaction>): Violation? =
            when {
                transactions.plus(transaction).sumBy(Transaction::amount) > acc.availableLimit -> Violation.INSUFFICIENT_LIMIT
                else -> null
            }

    /*
     "Doubled" transactions
     There should not be more than 2 transactions (exactly 2 is fine, based on the wording) on
     a 2 minute interval.

     Groups similar transactions together O(n),
     Then uses the EventFrequencyAnalyzer class on each group to detect if there were more than
     the allowed number of transactions in the timeframe. O(m) , where sum(m) = n
     */
    private fun checkDoubled(transaction: Transaction, transactions: List<Transaction>): Violation? {
        val buffer: MutableMap<UUID, MutableList<Instant>> = mutableMapOf()
        transactions.plus(transaction).forEach { analyzedTransaction ->
            //Uses a hashing function to determine what transactions are "similar" (i.e. same name
            // and amount) and groups them in a list
            val hash = UUID.nameUUIDFromBytes(
                    "${analyzedTransaction.merchant}|${analyzedTransaction.amount}".toByteArray())
            buffer.computeIfAbsent(hash) { mutableListOf() }
            buffer.get(hash)?.add(analyzedTransaction.time)
        }
        // Analyzes all lists of "similar" transactions to see if they occur more frequently than
        // the business rule allows
        // This is O(n) amortized, as the sum of sizes of all calls to the algorithm is equal to n
        val result = buffer.any { (_, list) ->
            frequencyAnalyser
                    .detectTransactionFrequencyExceededInTimeFrame(list,
                            MAX_SIMILAR_TRANSACTION_FOR_TIME_FRAME,
                            TIME_FRAME_IN_MINS_FOR_SIMILAR_TRANSACTION)
        }
        return if (result) { Violation.DOUBLED_TRANSACTION } else {null}
    }

    /*
     High-frequency-small-interval check
     There should not be more than 3 transactions on a 2 minute interval.
     Uses the EventFrequencyAnalyzer class to detect if there were more than
     the allowed number of transactions in the timeframe.
     */
    private fun checkFrequency(transaction: Transaction, transactions: List<Transaction>): Violation? {
        val events = transactions.plus(transaction).map {it.time }
        val result: Boolean = frequencyAnalyser
                .detectTransactionFrequencyExceededInTimeFrame(events,
                        MAX_FREQUENCY_FOR_TIME_FRAME,
                        TIME_FRAME_IN_MINS_FOR_FREQUENCY)
        return if (result) { Violation.HIGH_FREQUENCY_SMALL_INTERVAL } else { null }
    }

}