package bank.models

data class Account(val activeCard: Boolean,
                   val availableLimit: Int,
                   val id: String)