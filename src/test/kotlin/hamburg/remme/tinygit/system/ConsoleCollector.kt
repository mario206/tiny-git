package hamburg.remme.tinygit.system

internal class ConsoleCollector {

    val lines: List<String> = mutableListOf()

    fun collect(line: String) {
        lines as MutableList += line
    }

    override fun toString(): String = lines.joinToString("\n")

}
