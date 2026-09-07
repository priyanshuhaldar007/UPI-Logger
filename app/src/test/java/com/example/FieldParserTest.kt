package com.example

import com.example.parser.FieldParser
import com.example.parser.ParsedTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldParserTest {

    @Test
    fun testParseScreenA() {
        val screenAText = """
            Payment Successful
            Rahul Verma
            ₹48
            Paid to: rahul98@oksbi
            September 5 at 9:38 PM
            Axis CC XX87
            UPI Reference ID: 424881920384
            View more
        """.trimIndent()

        val parsed = FieldParser.parse(screenAText)

        assertEquals("₹48", parsed.amount)
        assertEquals("September 5 at 9:38 PM", parsed.date)
        assertEquals("rahul98@oksbi", parsed.vpa)
        assertEquals("Rahul Verma", parsed.payee)
        assertEquals("424881920384", parsed.referenceNumber)
        assertEquals("Axis CC XX87", parsed.paymentMethod)
        assertEquals("", parsed.note)
        assertEquals(ParsedTransaction.ScreenType.SCREEN_A, parsed.detectedScreenType)
    }

    @Test
    fun testParseScreenB() {
        val screenBText = """
            Details
            Date and time
            5 September 2026 • 09:38PM
            Payment Method
            Axis CC XX87
            Paid to
            rahul98@oksbi
            Note
            Dinner with family
            UPI Transaction Id
            424881920384
            Super.Money transaction ID
            SMTX9988112233
        """.trimIndent()

        val parsed = FieldParser.parse(screenBText)

        assertEquals("Dinner with family", parsed.note)
        assertEquals("424881920384", parsed.referenceNumber)
        assertEquals("SMTX9988112233", parsed.superMoneyTransactionId)
        assertEquals("Axis CC XX87", parsed.paymentMethod)
        assertEquals("rahul98@oksbi", parsed.vpa)
        assertTrue(parsed.date.contains("5 September 2026"))
        assertEquals(ParsedTransaction.ScreenType.SCREEN_B, parsed.detectedScreenType)
    }

    @Test
    fun testFallbackHeuristics() {
        val textWithFallbacks = """
            Transfer completed
            Paid: Rs. 1500
            Date: 12/10/2026
            Recipient: friend@upi
            Txn Ref: 987654321012
            Remarks: Monthly maintenance
        """.trimIndent()

        val parsed = FieldParser.parse(textWithFallbacks)

        assertEquals("₹1500", parsed.amount)
        assertEquals("12/10/2026", parsed.date)
        assertEquals("friend@upi", parsed.vpa)
        assertEquals("987654321012", parsed.referenceNumber)
        assertEquals("Monthly maintenance", parsed.note)
    }

    @Test
    fun testNoteRejectsNumberAtLettersAndPicksNextLine() {
        val ocrText = """
            Details
            Payment Method
            Axis CC XX87
            Paid to
            rahul98@oksbi
            Note
            9876543210@ybl
            Team dinner at Taj
            UPI Transaction Id
            424881920384
        """.trimIndent()

        val parsed = FieldParser.parse(ocrText)

        assertEquals("Team dinner at Taj", parsed.note)
        assertEquals("424881920384", parsed.referenceNumber)
    }

    @Test
    fun testNoteRejectsNumberAtLettersAndPicksPreviousLine() {
        val ocrText = """
            Details
            Payment Method
            Axis CC XX87
            Paid to
            rahul98@oksbi
            Office coffee and snacks
            Note
            9876543210@ybl
            UPI Transaction Id
            424881920384
        """.trimIndent()

        val parsed = FieldParser.parse(ocrText)

        assertEquals("Office coffee and snacks", parsed.note)
        assertEquals("424881920384", parsed.referenceNumber)
    }

    @Test
    fun testInlineNoteRejectsNumberAtLettersAndPicksNextLine() {
        val ocrText = """
            Paid to
            rahul98@oksbi
            Note: 9876543210@ybl
            Birthday gift contribution
            UPI Reference ID: 424881920384
        """.trimIndent()

        val parsed = FieldParser.parse(ocrText)

        assertEquals("Birthday gift contribution", parsed.note)
        assertEquals("424881920384", parsed.referenceNumber)
    }

    @Test
    fun testInlineNoteRejectsNumberAtLettersAndPicksPreviousLine() {
        val ocrText = """
            Paid to
            rahul98@oksbi
            Quarterly rent payment
            Note: 9876543210@ybl
            UPI Reference ID: 424881920384
        """.trimIndent()

        val parsed = FieldParser.parse(ocrText)

        assertEquals("Quarterly rent payment", parsed.note)
        assertEquals("424881920384", parsed.referenceNumber)
    }

    @Test
    fun testHorizontalGroupedNote() {
        val ocrText = """
            Details
            Payment Method
            Axis CC XX87
            Paid to
            rahul98@oksbi
            Note Dinner with family
            UPI Transaction Id
            424881920384
        """.trimIndent()

        val parsed = FieldParser.parse(ocrText)

        assertEquals("Dinner with family", parsed.note)
        assertEquals("424881920384", parsed.referenceNumber)
    }

    @Test
    fun testTopPayeeExtractionExcludesAvatarInitialAndHelp() {
        val ocrText = """
            Help
            R
            Rohan Sharma
            ₹150
            Paid to: rohan@okhdfcbank
            September 7 at 10:00 AM
            Axis CC XX87
            UPI Reference ID: 998877665544
        """.trimIndent()

        val parsed = FieldParser.parse(ocrText)

        assertEquals("Rohan Sharma", parsed.payee)
        assertEquals("₹150", parsed.amount)
        assertEquals("rohan@okhdfcbank", parsed.vpa)
        assertEquals("998877665544", parsed.referenceNumber)
    }

    @Test
    fun testAmountExtractionStopsAtPastTransactions() {
        val ocrText = """
            Payment Successful
            Amit Patel
            ₹500
            Paid to: amit@oksbi
            UPI Reference ID: 112233445566
            Past Transactions
            ₹1200
            ₹2400
        """.trimIndent()

        val parsed = FieldParser.parse(ocrText)

        assertEquals("Amit Patel", parsed.payee)
        assertEquals("₹500", parsed.amount)
        assertEquals("112233445566", parsed.referenceNumber)
    }

    @Test
    fun testAmountExtractionBreaksOnPastTransactionsWhenEmpty() {
        val ocrText = """
            Payment Successful
            Amit Patel
            Paid to: amit@oksbi
            UPI Reference ID: 112233445566
            Past transactions
            ₹1200
        """.trimIndent()

        val parsed = FieldParser.parse(ocrText)

        assertEquals("Amit Patel", parsed.payee)
        assertEquals("", parsed.amount)
        assertEquals("112233445566", parsed.referenceNumber)
    }
}

