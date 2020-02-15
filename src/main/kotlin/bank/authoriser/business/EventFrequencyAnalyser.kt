package bank.authoriser.business

import java.time.Duration
import java.time.Instant
import kotlin.math.abs

class EventFrequencyAnalyser {

    // Given a list of timestamps of events, a time frame in minutes and the maximum amount of
    // events, this method detects if the list did not exceed the amount of events per time frame.
    //
    // Algorithm:
    // Inserts every transaction on a circular buffer of size equal to the `max amount` of events
    // allowed in a round-robin fashion, that way, we're looking at up to the `max amount`
    // most recent events.
    //
    // At any given time, the difference between the current transaction and the oldest transaction
    // in the buffer needs to be greater than the time frame (otherwise, we have had too many
    // transactions in the time frame.)
    //
    // This algorithm is O(n).
    fun detectTransactionFrequencyExceededInTimeFrame(events: List<Instant>,
                                   maxFrequencyCount: Int,
                                   timeFrameInMins: Int): Boolean {
        if (events.size < maxFrequencyCount) {
            return false
        }
        val circularBuffer: Array<Instant?> = Array(maxFrequencyCount) { null }
        events.forEachIndexed { index, analyzedTransaction ->
            val oldestTransactionIndex = index % maxFrequencyCount
            val oldestTransactionTime = circularBuffer[oldestTransactionIndex] ?: Instant.MIN
            if (abs(Duration.between(analyzedTransaction, oldestTransactionTime).toMinutes()) <= timeFrameInMins) {
                return true

            }
            circularBuffer[oldestTransactionIndex] = analyzedTransaction
        }
        return false
    }
}