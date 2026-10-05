package com.example.ui.util

import kotlin.math.abs

fun formatIndianCurrency(amount: Number): String {
    val longVal = amount.toLong()
    val isNegative = longVal < 0
    val numStr = abs(longVal).toString()
    if (numStr.length <= 3) return if (isNegative) "-$numStr" else numStr
    val lastThree = numStr.takeLast(3)
    val otherDigits = numStr.dropLast(3)
    val formattedOther = otherDigits.reversed().chunked(2).joinToString(",").reversed()
    val result = "$formattedOther,$lastThree"
    return if (isNegative) "-$result" else result
}
