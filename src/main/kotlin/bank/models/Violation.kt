package bank.models

import com.fasterxml.jackson.annotation.JsonValue

enum class Violation (@JsonValue val code: String) {
    ACCOUNT_ALREADY_INITIALIZED("account-already-initialized"),
    INSUFFICIENT_LIMIT("insufficient-limit"),
    CARD_NOT_ACTIVE("card-not-active"),
    HIGH_FREQUENCY_SMALL_INTERVAL("high-frequency-small-interval"),
    DOUBLED_TRANSACTION("doubled-transaction"),
    ACCOUNT_DOES_NOT_EXIST("account-does-not-exist")
}