package bank.authoriser

import bank.authoriser.business.Authoriser
import bank.authoriser.business.EventFrequencyAnalyser
import bank.models.Account
import bank.models.Transaction
import bank.models.Violation
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthoriserTest {

    private fun getTime(hour: Int = 0, minute: Int = 0, second: Int = 0) =
            ZonedDateTime.of(2019, 10, 1, hour, minute, second, 0, ZoneId.of("UTC")).toInstant()

    private val frequencyAnalyzer = EventFrequencyAnalyser()

    @Test fun `authorizes transaction within account limit`() {
        //Given
        val acc = Account(true, 100, "ACC_A")
        //When
        val result = Authoriser(frequencyAnalyzer).transaction(
                acc, Transaction("ACC_A", "Habibs", 10, getTime()), emptyList())
        //Then
        assertTrue("Result was ${result.violations.joinToString(",")}") {
            result.violations.isEmpty()
        }
    }

    @Test fun `detects transaction above account limit`() {
        //Given
        val acc = Account(true, 100, "ACC_A")
        //When
        val result = Authoriser(frequencyAnalyzer).transaction(
                acc, Transaction("ACC_A", "Habibs", 120, getTime()), emptyList())
        //Then
        assertTrue("Result was ${result.violations.joinToString(",")}") {
            result.violations contentEquals arrayOf(Violation.INSUFFICIENT_LIMIT)
        }
    }

    @Test fun `detects transaction above account limit - over multiple transactions`() {
        //Given
        val acc = Account(true, 100, "ACC_A")
        val authoriser = Authoriser(frequencyAnalyzer)
        authoriser.transaction(acc, Transaction("ACC_A", "Habibs", 40, getTime(9, 0, 0)), emptyList())
        authoriser.transaction(acc, Transaction("ACC_A", "Zara", 60, getTime(9, 1, 0)), emptyList())
        //When
        val result = authoriser.transaction(acc, Transaction("ACC_A", "Spotify", 20, getTime(9, 1, 39)), emptyList())
        //Then
        assertTrue("Result was ${result.violations.joinToString(",")}") {
            result.violations contentEquals arrayOf(Violation.INSUFFICIENT_LIMIT)
        }
    }

    @Test fun `detects transaction when card is not active`() {
        //Given
        val acc = Account(false, 1000, "ACC_A")

        //When
        val result = Authoriser(frequencyAnalyzer).transaction(
                acc, Transaction("ACC_A", "Habibs", 10, getTime()), emptyList())
        //Then
        assertTrue("Result was ${result.violations.joinToString(",")}") {
            result.violations contentEquals arrayOf(Violation.CARD_NOT_ACTIVE)
        }
    }

    @Test fun `detects doubled transaction - identical`() {
        //Given
        val acc = Account(true, 100, "ACC_A")
        val authoriser = Authoriser(frequencyAnalyzer)
        val firstTransactionTime = getTime(9,0,0)
        authoriser.transaction(acc, Transaction("ACC_A", "Habibs", 20, firstTransactionTime), emptyList())
        authoriser.transaction(acc, Transaction("ACC_A", "Habibs", 20, firstTransactionTime), emptyList())
        //When
        val result = authoriser.transaction(
                acc, Transaction("ACC_A", "Habibs", 20, firstTransactionTime), emptyList())

        //Then
        assertTrue("Result was ${result.violations.joinToString(",")}") {
            result.violations contentEquals arrayOf(Violation.DOUBLED_TRANSACTION)
        }
    }

    @Test fun `detects doubled transaction - two mins apart`() {
        //Given
        val acc = Account(true, 100, "ACC_A")
        val authoriser = Authoriser(frequencyAnalyzer)
        authoriser.transaction(acc, Transaction("ACC_A", "Habibs",
                20,
                getTime(9, 0, 0)), emptyList())

        authoriser.transaction(acc, Transaction("ACC_A", "Habibs",
                20,
                getTime(9, 1, 0)), emptyList())
        //When
        val result = authoriser.transaction(acc, Transaction("ACC_A", "Habibs",
                20,
                getTime(9, 2, 0)), emptyList())
        //Then
        assertTrue("Result was ${result.violations.joinToString(",")}") {
            result.violations contentEquals arrayOf(Violation.DOUBLED_TRANSACTION)
        }
    }

    @Test fun `allows "doubled" transaction if further than 2 mins apart`() {
        //Given
        val acc = Account(true, 100, "ACC_A")
        val authoriser = Authoriser(frequencyAnalyzer)
        authoriser.transaction(acc, Transaction("ACC_A", "Habibs",
                20,
                getTime(9, 0, 0)), emptyList())
        //When
        val result = authoriser.transaction(acc, Transaction("ACC_A", "Habibs",
                20,
                getTime(9, 3, 0)), emptyList())
        //Then
        assertTrue("Result was ${result.violations.joinToString(",")}}") {
            result.violations.isEmpty()
        }
    }

    @Test fun `detects high frequency over small period`() {
        //Given
        val acc = Account(true, 100, "ACC_A")
        val authoriser = Authoriser(frequencyAnalyzer)
        authoriser.transaction(acc, Transaction("ACC_A", "Habibs", 10, getTime(9, 0, 0)), emptyList())
        authoriser.transaction(acc, Transaction("ACC_A", "Zara", 60, getTime(9, 0, 30)), emptyList())
        authoriser.transaction(acc, Transaction("ACC_A", "Spotify", 10, getTime(9, 1, 30)), emptyList())

        //When
        val result = authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 10, getTime(9, 2, 0)), emptyList())
        assertTrue("Result was ${result.violations.joinToString(",")}") {
            result.violations contentEquals arrayOf(Violation.HIGH_FREQUENCY_SMALL_INTERVAL)
        }
    }

    @Test fun `detects more than one violation on the same transaction - insufficient limit & high frequency small interval`() {
        //Given
        val acc = Account(true, 100, "ACC_A")
        val authoriser = Authoriser(frequencyAnalyzer)
        val transactionHistory = listOf(Transaction("ACC_A", "Habibs", 20, getTime(9, 0, 0)),
                Transaction("ACC_A", "Zara", 60, getTime(9, 0, 30)),
                Transaction("ACC_A", "Netflix", 20, getTime(9, 1, 0)))
        //When
        val result = authoriser.transaction(acc,
                Transaction("ACC_A", "Spotify", 10, getTime(9, 1, 30)),
                transactionHistory)
        //Then
        assertTrue("Result was ${result.violations.joinToString(",")}") {
            result.violations.contains(Violation.INSUFFICIENT_LIMIT)
                    && result.violations.contains(Violation.HIGH_FREQUENCY_SMALL_INTERVAL)
        }
    }

    @Test fun `detects more than one violation on the same transaction -  high frequency small interval & doubled transaction`() {
        //Given
        val acc = Account(true, 100, "ACC_A")
        val authoriser = Authoriser(frequencyAnalyzer)
        authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 20, getTime()), emptyList())
        authoriser.transaction(acc, Transaction("ACC_A", "Habibs", 10, getTime()), emptyList())
        authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 20, getTime()), emptyList())
        //When
        val result = authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 20, getTime()), emptyList())
        //Then
        assertTrue("Result was ${result.violations.joinToString(",")}") {
            result.violations.contains(Violation.HIGH_FREQUENCY_SMALL_INTERVAL)
                    && result.violations.contains(Violation.DOUBLED_TRANSACTION)
        }
    }

    @Test fun `does not consider transactions that were violated after a while`() {
        //Given
        val acc = Account(true, 120, "ACC_A")
        val authoriser = Authoriser(frequencyAnalyzer)
        authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 40, getTime(9, 0, 0)), emptyList())
        authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 40, getTime(9, 1, 0)), emptyList())
        //Now a transaction that is a violation
        val firstResult = authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 40, getTime(9, 1, 30)), emptyList())
        assertTrue(firstResult.violations.contains(Violation.DOUBLED_TRANSACTION))

        //When
        val finalResult = authoriser.transaction(acc, Transaction("ACC_A", "Habibs", 10, getTime(9, 3, 0)), emptyList())

        //Then
        //The final result is accepted
        assertTrue(finalResult.violations.isEmpty())
    }

    @Test fun `returns updated account info`() {
        val acc = Account(true, 100, "ACC_A")
        val authoriser = Authoriser(frequencyAnalyzer)
        assertEquals(90, authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 10, getTime(9,0,0)), emptyList()).account?.availableLimit)
        assertEquals(50, authoriser.transaction(acc, Transaction("ACC_A", "Loja da Esquina", 40, getTime(9,10,0)), emptyList()).account?.availableLimit)
        assertEquals(5, authoriser.transaction(acc, Transaction("ACC_A", "Loterica", 45, getTime(9,50,0)), emptyList()).account?.availableLimit)
    }

    @Test fun `passes a simulation over time`() {
        val acc = Account(true, 120, "ACC_A")
        val authoriser = Authoriser(frequencyAnalyzer)
        assertEmpty(authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 20, getTime(9, 0, 0)), emptyList()).violations)
        assertEmpty(authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 20, getTime(9, 0, 0)), emptyList()).violations)

        // violates
        assertNotEmpty(authoriser.transaction(acc, Transaction("ACC_A", "Netflix", 20, getTime(9, 0, 0)), emptyList()).violations)

        assertEmpty(authoriser.transaction(acc, Transaction("ACC_A", "Habibs", 30, getTime(9, 10, 0)), emptyList()).violations)

        assertEmpty(authoriser.transaction(acc, Transaction("ACC_A", "Recarga", 10, getTime(9, 30, 0)), emptyList()).violations)
        assertEmpty(authoriser.transaction(acc, Transaction("ACC_A", "Restaurante", 20, getTime(9, 30, 15)), emptyList()).violations)
        assertEmpty(authoriser.transaction(acc, Transaction("ACC_A", "Bar", 10, getTime(9, 30, 30)), emptyList()).violations)

        // violates
        assertNotEmpty(authoriser.transaction(acc, Transaction("ACC_A", "Bar", 10, getTime(9, 30, 45)), emptyList()).violations)

        //violates
        assertNotEmpty(authoriser.transaction(acc, Transaction("ACC_A", "Bar", 10, getTime(9, 32, 0)), emptyList()).violations)

        val result = authoriser.transaction(acc, Transaction("ACC_A", "Uber", 10, getTime(9, 40, 0)), emptyList())
        assertEquals(0, result.account?.availableLimit)

    }

    fun <T> assertEmpty(arr: Array<T>) {
        assertTrue("Expected empty, but was ${arr.joinToString(",")}") { arr.isEmpty() }
    }

    fun <T> assertNotEmpty(arr: Array<T>) {
        assertFalse("Expected not-empty, but was empty") { arr.isEmpty() }
    }
}