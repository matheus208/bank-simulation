package bank.models

import java.time.Instant

data class Transaction(val accountId: String,
                       val merchant: String,
                       val amount: Int,
                       val time: Instant)