package com.vitor.melembre.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderUrlTest {
    @Test
    fun blankBecomesEmpty() {
        assertEquals("", ReminderUrl.normalize("   "))
        assertEquals("", ReminderUrl.normalize(null))
    }

    @Test
    fun addsHttpsWhenSchemeMissing() {
        assertEquals("https://servicos.pf.gov.br/x", ReminderUrl.normalize(" servicos.pf.gov.br/x "))
    }

    @Test
    fun keepsHttpAndHttps() {
        assertEquals("http://a.com", ReminderUrl.normalize("http://a.com"))
        assertEquals("https://a.com", ReminderUrl.normalize("https://a.com"))
    }

    @Test
    fun rejectsDangerousOrBroken() {
        assertNull(ReminderUrl.normalize("javascript:alert(1)"))
        assertNull(ReminderUrl.normalize("file:///etc/passwd"))
        assertNull(ReminderUrl.normalize("https://"))
        assertNull(ReminderUrl.normalize("a b.com"))
    }
}
