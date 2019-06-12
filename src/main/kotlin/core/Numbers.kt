package core

import java.math.BigDecimal

fun Pair<Int, Int>.percentage(): Double {
    return if (first == 0 && second == 0) 0.0
    else ((first.toDouble() / (second + first)) * HUNDRED)
}

const val THOUSAND = 1000.0
const val HUNDRED = 100

fun Double.round(scale : Int) =
        BigDecimal(this).setScale(scale, BigDecimal.ROUND_HALF_UP).toDouble()

fun Number.isNotGreaterThan(number: Number?): Boolean {
    if (number == null) return true
    return this.toDouble() <= number.toDouble()
}

fun Number.isNotLessThan(number: Number?): Boolean {
    if (number == null) return true
    return this.toDouble() >= number.toDouble()
}

fun Int.blankOrNum() : String = if (this == 0) "" else "$this"

fun <T, V> Map<V?, List<T>>.entryChildrenSum(): Int {
    return flatMap { entry -> listOf(entry.value.count()) }.sum()
}
