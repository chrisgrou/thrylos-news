package gr.thrylos.news.sources.testutil

object Fixtures {
    fun read(name: String): String =
        Fixtures::class.java.classLoader!!.getResourceAsStream("fixtures/$name")!!
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
}
