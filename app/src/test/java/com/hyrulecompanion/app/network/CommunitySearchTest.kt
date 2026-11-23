package com.hyrulecompanion.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunitySearchTest {
    @Test
    fun shortQueriesOnlyMatchLiteralSubstrings() {
        assertEquals(setOf("*l\\_*"), relatedHandlePatterns("l_"))
    }

    @Test
    fun longerQueriesIncludeSingleEditsAndTranspositions() {
        val patterns = relatedHandlePatterns("lnik")
        assertTrue("link*" in patterns)
        assertTrue("l_ik*" in patterns)
        assertTrue("lik*" in patterns)
        assertTrue("l_nik*" in patterns)
        assertTrue("*lnik*" in patterns)
    }
}
