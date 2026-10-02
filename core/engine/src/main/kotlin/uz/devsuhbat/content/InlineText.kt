package uz.devsuhbat.content

data class InlineSegment(val text: String, val code: Boolean)

/** Splits content text into plain and `inline code` segments. */
object InlineText {
    private const val TICK = '`'

    /** An unpaired backtick and everything after it stays literal; empty code spans are dropped. */
    fun parse(text: String): List<InlineSegment> {
        val segments = mutableListOf<InlineSegment>()
        val plain = StringBuilder()

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                segments += InlineSegment(plain.toString(), code = false)
                plain.clear()
            }
        }

        var index = 0
        while (index < text.length) {
            val open = text.indexOf(TICK, index)
            val close = if (open >= 0) text.indexOf(TICK, open + 1) else -1
            if (close < 0) {
                plain.append(text, index, text.length)
                break
            }
            plain.append(text, index, open)
            if (close > open + 1) {
                flushPlain()
                segments += InlineSegment(text.substring(open + 1, close), code = true)
            }
            index = close + 1
        }
        flushPlain()
        return segments
    }
}
