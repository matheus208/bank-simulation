package bank.authoriser

import bank.authoriser.business.EventFrequencyAnalyser
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventFrequencyAnalyserTest {
    private fun getTime(hour: Int = 0, minute: Int = 0, second: Int = 0) =
            ZonedDateTime.of(2019, 10, 1, hour, minute, second, 0, ZoneId.of("UTC")).toInstant()

    @Test
    fun `allows few events`() {
        //Given
        val events = listOf(getTime(9,0,0),
                getTime(9,1,0),
                getTime(9, 1, 30))
        //When
        val result = EventFrequencyAnalyser()
                .detectTransactionFrequencyExceededInTimeFrame(events, 10, 10)

        //Then
        assertFalse(result)
    }

    @Test fun `allows spreaded out events`() {
        //Given
        val events = listOf(getTime(5,0,0),
                getTime(5,10,0),
                getTime(5, 20, 0),
                getTime(5, 30, 0),
                getTime(5, 40, 0))
        //When
        val result = EventFrequencyAnalyser()
                .detectTransactionFrequencyExceededInTimeFrame(events, 2, 5)

        //Then
        assertFalse(result)
    }

    @Test fun `detects too many events`() {
        //Given
        val events = listOf(getTime(5,0,0),
                getTime(5,10,0),
                getTime(5, 20, 0),
                getTime(5, 30, 0),
                getTime(5, 40, 0))
        //When
        val result = EventFrequencyAnalyser()
                .detectTransactionFrequencyExceededInTimeFrame(events, 2, 30)

        //Then
        assertTrue(result)
    }

    @Test fun `detects too many events when the last one is the infractor`() {
        //Given
        val events = listOf(getTime(5,0,0),
                getTime(5,10,0),
                getTime(5, 20, 0),
                getTime(5, 30, 0),
                getTime(5, 40, 0))
        //When
        val result = EventFrequencyAnalyser()
                .detectTransactionFrequencyExceededInTimeFrame(events, 4, 60)

        //Then
        assertTrue(result)
    }

    @Test fun `allows an amount equal to the max`() {
        //Given
        val events = listOf(
                getTime(5,0,0),
                getTime(5,1,0),
                getTime(5, 2, 0))
        //When
        val result = EventFrequencyAnalyser()
                .detectTransactionFrequencyExceededInTimeFrame(events, 3, 2)

        //Then
        assertFalse(result)
    }

}