package com.vitor.melembre.data

enum class Recurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ;

    val labelPt: String
        get() = when (this) {
            NONE -> "Não repetir"
            DAILY -> "Todo dia"
            WEEKLY -> "Toda semana"
            MONTHLY -> "Todo mês"
            YEARLY -> "Todo ano"
        }

    val shortLabelPt: String?
        get() = when (this) {
            NONE -> null
            DAILY -> "Diário"
            WEEKLY -> "Semanal"
            MONTHLY -> "Mensal"
            YEARLY -> "Anual"
        }

    companion object {
        fun fromStorage(value: String?): Recurrence {
            return entries.firstOrNull { it.name == value } ?: NONE
        }
    }
}
