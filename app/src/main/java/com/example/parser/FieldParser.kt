package com.example.parser

import java.util.regex.Pattern

/**
 * Result of parsing text extracted from a UPI screenshot.
 */
data class ParsedTransaction(
    val amount: String = "",
    val date: String = "",
    val payee: String = "",
    val vpa: String = "",
    val referenceNumber: String = "",
    val paymentMethod: String = "",
    val note: String = "",
    val superMoneyTransactionId: String = "",
    val detectedScreenType: ScreenType = ScreenType.UNKNOWN
) {
    enum class ScreenType {
        SCREEN_A, // Transaction Result (Amount, Date, Payee, UPI Reference ID, Payment Method)
        SCREEN_B, // Details Bottom Sheet (Note, UPI Transaction Id, Super.Money ID, etc.)
        UNKNOWN
    }
}

/**
 * Isolated, strictly offline field parser designed for UPI screenshots (specifically super.money).
 *
 * Primary parsing uses exact label matching and formats from the real app.
 * Fallback heuristics are used only if exact labels are not matched.
 */
object FieldParser {

    // --- Regex Patterns for Exact and Fallback Rules ---

    // Amount: Currency ₹ followed by digits, optional comma and decimals
    private val AMOUNT_EXACT_REGEX = Pattern.compile("₹\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val AMOUNT_FALLBACK_REGEX = Pattern.compile("(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)

    // Date Screen A: [Month] [D] at [H]:[MM] [AM/PM] (e.g., September 5 at 9:38 PM)
    private val DATE_SCREEN_A_REGEX = Pattern.compile(
        "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{1,2}\\s+at\\s+\\d{1,2}:\\d{2}\\s*(?:AM|PM|am|pm)",
        Pattern.CASE_INSENSITIVE
    )

    // Date Screen B: [D] [Month] [YYYY] • [HH]:[MM][AM/PM] (• may be OCR'd as ·, ., -, etc.)
    private val DATE_SCREEN_B_REGEX = Pattern.compile(
        "\\d{1,2}\\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{4}\\s*[•·.\\-~*|]\\s*\\d{1,2}:\\d{2}\\s*(?:AM|PM|am|pm)?",
        Pattern.CASE_INSENSITIVE
    )

    // Generic Date Fallback: DD/MM/YYYY or DD Mon YYYY
    private val DATE_FALLBACK_REGEX = Pattern.compile(
        "(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})|(\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{2,4})",
        Pattern.CASE_INSENSITIVE
    )

    // VPA: text@text (e.g. q855253009@ybl)
    private val VPA_REGEX = Pattern.compile("[a-zA-Z0-9.\\-_]+@[a-zA-Z0-9.\\-_]+")

    // Reference ID: 10-14 digit number
    private val REF_NUM_REGEX = Pattern.compile("\\b(\\d{10,14})\\b")

    // Super.Money ID: long alphanumeric string often starting with SMTX
    private val SMTX_ID_REGEX = Pattern.compile("\\b(SMTX[a-zA-Z0-9]+|[A-Z0-9]{12,24})\\b")

    // Payment Method heuristic for Screen A (e.g. "Axis CC XX87", "HDFC Bank XX1234")
    private val PAYMENT_METHOD_HEURISTIC_REGEX = Pattern.compile(
        "(?i)^([A-Za-z0-9\\s&.-]+?(?:CC|DC|Bank|A/c|Account|Card)?\\s*(?:XX|X+|\\*{2,}|ending\\s+in)\\s*\\d{2,4})$"
    )

    // Series of number@letters pattern (e.g. 9876543210@ybl, q855253009@ybl, 12345@ibl)
    private val NUMBER_AT_LETTERS_REGEX = Pattern.compile(
        "(?i)\\b(?:[a-z0-9._-]*\\d+[a-z0-9._-]*@[a-z]+|[a-z]+@[a-z0-9._-]*\\d+[a-z0-9._-]*)\\b"
    )

    /**
     * Parses the raw OCR string and extracts transaction fields.
     */
    fun parse(rawText: String): ParsedTransaction {
        if (rawText.isBlank()) {
            return ParsedTransaction()
        }

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var parsedAmount = ""
        var parsedDate = ""
        var parsedPayee = ""
        var parsedVpa = ""
        var parsedRef = ""
        var parsedPaymentMethod = ""
        var parsedNote = ""
        var parsedSmtxId = ""

        var isScreenBIndicator = false
        var isScreenAIndicator = false

        // 1. Pass: Scan lines with exact label markers
        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase()

            // Check for Screen B specific markers
            if (lower == "note" || lower.startsWith("note:") || lower.contains("upi transaction id") || lower.contains("super.money transaction id")) {
                isScreenBIndicator = true
            }

            // Check for Screen A specific markers
            if (lower.contains("view more") || lower.contains("upi reference id") || lower.contains("payment successful") || lower.contains("paid successfully")) {
                isScreenAIndicator = true
            }

            // --- NOTE EXTRACTION (Screen B Highest Priority) ---
            // Exact label: "Note" on its own, or "Note:", or "Note <text>"
            if (lower == "note" || lower == "note:") {
                val candidate = if (i + 1 < lines.size) lines[i + 1] else ""
                val resolved = resolveValidNote(lines, candidateIndex = i + 1, labelIndex = i, initialCandidate = candidate)
                if (resolved.isNotEmpty()) {
                    parsedNote = resolved
                }
                if (parsedVpa.isEmpty() && isNumberAtLetters(candidate)) {
                    parsedVpa = extractVpa(candidate)
                }
            } else if (lower.startsWith("note:") && parsedNote.isEmpty()) {
                val inlineCandidate = line.substringAfter(":", "").trim()
                val resolved = resolveValidNote(lines, candidateIndex = i, labelIndex = i, initialCandidate = inlineCandidate)
                if (resolved.isNotEmpty()) {
                    parsedNote = resolved
                }
                if (parsedVpa.isEmpty() && isNumberAtLetters(inlineCandidate)) {
                    parsedVpa = extractVpa(inlineCandidate)
                }
            } else if (lower.startsWith("note ") && parsedNote.isEmpty() && line.length > 5) {
                val inlineCandidate = line.substring(4).trim()
                val resolved = resolveValidNote(lines, candidateIndex = i, labelIndex = i, initialCandidate = inlineCandidate)
                if (resolved.isNotEmpty()) {
                    parsedNote = resolved
                }
                if (parsedVpa.isEmpty() && isNumberAtLetters(inlineCandidate)) {
                    parsedVpa = extractVpa(inlineCandidate)
                }
            }

            // --- REFERENCE NUMBER EXTRACTION ---
            // Label "UPI Reference ID" (Screen A) or "UPI Transaction Id" (Screen B)
            if (lower.contains("upi reference id") || lower.contains("upi transaction id") || lower.contains("reference id")) {
                // First check same line (e.g. "UPI Reference ID: 424881920384")
                val inlineMatcher = REF_NUM_REGEX.matcher(line)
                if (inlineMatcher.find()) {
                    parsedRef = inlineMatcher.group(1) ?: ""
                } else if (i + 1 < lines.size) {
                    // Check next line
                    val nextMatcher = REF_NUM_REGEX.matcher(lines[i + 1])
                    if (nextMatcher.find()) {
                        parsedRef = nextMatcher.group(1) ?: ""
                    }
                }
            }

            // --- SUPER.MONEY TRANSACTION ID ---
            if (lower.contains("super.money transaction id") || lower.contains("super money transaction id")) {
                val inlineMatcher = SMTX_ID_REGEX.matcher(line)
                if (inlineMatcher.find()) {
                    parsedSmtxId = inlineMatcher.group(1) ?: ""
                } else if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1]
                    val nextMatcher = SMTX_ID_REGEX.matcher(nextLine)
                    if (nextMatcher.find()) {
                        parsedSmtxId = nextMatcher.group(1) ?: ""
                    } else if (nextLine.length in 8..30 && !isKnownLabel(nextLine)) {
                        parsedSmtxId = nextLine
                    }
                }
            }

            // --- PAYMENT METHOD EXTRACTION ---
            // Label "Payment Method" or "Payment method"
            if (lower.contains("payment method")) {
                val inlineValue = line.substringAfter(":", "").trim()
                if (inlineValue.isNotEmpty() && !inlineValue.equals("payment method", ignoreCase = true)) {
                    parsedPaymentMethod = inlineValue
                } else if (i + 1 < lines.size && !isKnownLabel(lines[i + 1])) {
                    parsedPaymentMethod = lines[i + 1]
                }
            }

            // --- PAYEE & VPA EXTRACTION ---
            // Label "Paid to" or "Paid to:"
            if (lower.startsWith("paid to") || lower == "paid to:" || lower == "paid to") {
                // Check if VPA or payee is on the same line
                val afterColon = line.substringAfter(":", "").trim()
                if (afterColon.isNotEmpty()) {
                    if (VPA_REGEX.matcher(afterColon).find()) {
                        parsedVpa = extractVpa(afterColon)
                    } else if (parsedPayee.isEmpty() && !isKnownLabel(afterColon) && afterColon.length in 2..40) {
                        parsedPayee = afterColon
                    }
                }

                // If VPA not on same line, look at subsequent lines for VPA
                if (parsedVpa.isEmpty()) {
                    for (offset in 1..2) {
                        if (i + offset < lines.size) {
                            val targetLine = lines[i + offset]
                            val vpaMatcher = VPA_REGEX.matcher(targetLine)
                            if (vpaMatcher.find()) {
                                parsedVpa = vpaMatcher.group(0) ?: ""
                                break
                            }
                        }
                    }
                }

                // If payee name appears above "Paid to" as a header on Screen A
                if (parsedPayee.isEmpty() && i > 0) {
                    for (back in 1..3) {
                        if (i - back >= 0) {
                            val candidate = lines[i - back]
                            val candLower = candidate.lowercase()
                            if (!isKnownLabel(candidate) &&
                                !candidate.contains("₹") &&
                                !candidate.contains("INR", ignoreCase = true) &&
                                !candidate.contains("Rs", ignoreCase = true) &&
                                !candidate.any { it.isDigit() } &&
                                !AMOUNT_EXACT_REGEX.matcher(candidate).find() &&
                                !AMOUNT_FALLBACK_REGEX.matcher(candidate).find() &&
                                !candLower.contains("payment successful") &&
                                !candLower.contains("paid successfully") &&
                                !candLower.contains("super.money") &&
                                !candidate.contains("@") &&
                                !DATE_SCREEN_A_REGEX.matcher(candidate).find() &&
                                !DATE_SCREEN_B_REGEX.matcher(candidate).find() &&
                                candidate.length in 2..40
                            ) {
                                parsedPayee = candidate
                                break
                            }
                        }
                    }
                }
            }

            // --- AMOUNT EXTRACTION ---
            if (parsedAmount.isEmpty()) {
                val amountMatcher = AMOUNT_EXACT_REGEX.matcher(line)
                if (amountMatcher.find()) {
                    parsedAmount = "₹" + (amountMatcher.group(1) ?: "")
                }
            }

            // --- DATE EXTRACTION (Screen A or Screen B format) ---
            if (parsedDate.isEmpty()) {
                val dateBMatcher = DATE_SCREEN_B_REGEX.matcher(line)
                if (dateBMatcher.find()) {
                    parsedDate = dateBMatcher.group(0) ?: ""
                } else {
                    val dateAMatcher = DATE_SCREEN_A_REGEX.matcher(line)
                    if (dateAMatcher.find()) {
                        parsedDate = dateAMatcher.group(0) ?: ""
                    }
                }
            }
        }

        // --- 2. Pass: Fallback heuristics for uncaptured fields ---

        // Fallback for Amount
        if (parsedAmount.isEmpty()) {
            val fallbackAmountMatcher = AMOUNT_FALLBACK_REGEX.matcher(rawText)
            if (fallbackAmountMatcher.find()) {
                parsedAmount = "₹" + (fallbackAmountMatcher.group(1) ?: "")
            }
        }

        // Fallback for Date
        if (parsedDate.isEmpty()) {
            val fallbackDateMatcher = DATE_FALLBACK_REGEX.matcher(rawText)
            if (fallbackDateMatcher.find()) {
                parsedDate = fallbackDateMatcher.group(0) ?: ""
            }
        }

        // Fallback for VPA
        if (parsedVpa.isEmpty()) {
            val vpaMatcher = VPA_REGEX.matcher(rawText)
            if (vpaMatcher.find()) {
                parsedVpa = vpaMatcher.group(0) ?: ""
            }
        }

        // Fallback for Reference Number if not found by label
        if (parsedRef.isEmpty()) {
            // Look for any 12-digit number (standard UPI UTR length is 12 digits)
            for (line in lines) {
                if (line.lowercase().contains("ref") || line.lowercase().contains("utr") || line.lowercase().contains("txn")) {
                    val matcher = REF_NUM_REGEX.matcher(line)
                    if (matcher.find()) {
                        parsedRef = matcher.group(1) ?: ""
                        break
                    }
                }
            }
        }

        // Fallback for Note: Look for "Remark", "Message", "For", "Note:"
        if (parsedNote.isEmpty()) {
            for (i in lines.indices) {
                val lower = lines[i].lowercase()
                if (lower.startsWith("remark:") || lower.startsWith("remarks:") ||
                    lower.startsWith("message:") || lower.startsWith("for:")
                ) {
                    val inlineCandidate = lines[i].substringAfter(":", "").trim()
                    val candidate = if (inlineCandidate.isNotEmpty()) inlineCandidate else if (i + 1 < lines.size) lines[i + 1] else ""
                    val candidateIdx = if (inlineCandidate.isNotEmpty()) i else i + 1
                    val resolved = resolveValidNote(lines, candidateIndex = candidateIdx, labelIndex = i, initialCandidate = candidate)
                    if (resolved.isNotEmpty()) {
                        parsedNote = resolved
                        break
                    }
                } else if (lower == "remark" || lower == "remarks" || lower == "message") {
                    val candidate = if (i + 1 < lines.size) lines[i + 1] else ""
                    val resolved = resolveValidNote(lines, candidateIndex = i + 1, labelIndex = i, initialCandidate = candidate)
                    if (resolved.isNotEmpty()) {
                        parsedNote = resolved
                        break
                    }
                }
            }
        }

        // Fallback for Payment Method (Screen A often shows card/account e.g. "Axis CC XX87" without label)
        if (parsedPaymentMethod.isEmpty()) {
            for (line in lines) {
                val trimmed = line.trim()
                if (!isKnownLabel(trimmed) && trimmed != parsedPayee && trimmed != parsedNote && !trimmed.contains("@")) {
                    val matcher = PAYMENT_METHOD_HEURISTIC_REGEX.matcher(trimmed)
                    if (matcher.find()) {
                        parsedPaymentMethod = matcher.group(1)?.trim() ?: ""
                        break
                    }
                }
            }
        }

        // Post-processing safeguard: note CANNOT be a string containing a series of number@letters
        // If so, check adjacent lines (next or previous) for a meaningful word
        if (parsedNote.isNotEmpty() && isNumberAtLetters(parsedNote)) {
            val noteLineIdx = lines.indexOfFirst { it.contains(parsedNote) }
            parsedNote = if (noteLineIdx != -1) {
                findMeaningfulWordAdjacent(lines, noteLineIdx)
            } else {
                ""
            }
        }

        // Determine detected screen type
        val screenType = when {
            isScreenBIndicator || (parsedNote.isNotEmpty() && parsedAmount.isEmpty()) ->
                ParsedTransaction.ScreenType.SCREEN_B
            isScreenAIndicator || (parsedAmount.isNotEmpty() && parsedNote.isEmpty()) ->
                ParsedTransaction.ScreenType.SCREEN_A
            parsedAmount.isNotEmpty() ->
                ParsedTransaction.ScreenType.SCREEN_A
            parsedNote.isNotEmpty() ->
                ParsedTransaction.ScreenType.SCREEN_B
            else ->
                ParsedTransaction.ScreenType.UNKNOWN
        }

        return ParsedTransaction(
            amount = parsedAmount,
            date = parsedDate,
            payee = parsedPayee,
            vpa = parsedVpa,
            referenceNumber = parsedRef,
            paymentMethod = parsedPaymentMethod,
            note = parsedNote,
            superMoneyTransactionId = parsedSmtxId,
            detectedScreenType = screenType
        )
    }

    /**
     * Checks if a string contains a series of number@letters (such as 9876543210@ybl, q855253009@ybl, 123@ibl)
     * or any UPI handle pattern with numbers and letters.
     */
    fun isNumberAtLetters(text: String): Boolean {
        val trimmed = text.trim()
        if (!trimmed.contains("@")) return false
        if (NUMBER_AT_LETTERS_REGEX.matcher(trimmed).find()) return true

        val tokens = trimmed.split("\\s+".toRegex())
        for (token in tokens) {
            if (token.contains("@")) {
                val before = token.substringBefore("@")
                val after = token.substringAfter("@")
                if ((before.any { it.isDigit() } && after.any { it.isLetter() }) ||
                    (before.any { it.isLetter() } && after.any { it.isDigit() })
                ) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Determines if a string is a meaningful word/phrase suitable for a transaction note or remark.
     * Excludes UI labels, series of number@letters, amounts, dates, IDs, and system status strings.
     */
    fun isMeaningfulWord(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < 2) return false
        if (isKnownLabel(trimmed)) return false
        if (isNumberAtLetters(trimmed)) return false
        if (VPA_REGEX.matcher(trimmed).matches()) return false
        if (AMOUNT_EXACT_REGEX.matcher(trimmed).find() || AMOUNT_FALLBACK_REGEX.matcher(trimmed).find()) return false
        if (trimmed.startsWith("₹") || trimmed.startsWith("Rs", ignoreCase = true) || trimmed.startsWith("INR", ignoreCase = true)) return false
        if (DATE_SCREEN_A_REGEX.matcher(trimmed).find() || DATE_SCREEN_B_REGEX.matcher(trimmed).find() || DATE_FALLBACK_REGEX.matcher(trimmed).find()) return false
        if (REF_NUM_REGEX.matcher(trimmed).matches()) return false
        if (SMTX_ID_REGEX.matcher(trimmed).matches()) return false
        if (trimmed.all { it.isDigit() || it.isWhitespace() || it == '-' || it == ':' || it == '.' || it == '/' }) return false

        val lower = trimmed.lowercase()
        if (lower.contains("payment successful") || lower.contains("paid successfully") ||
            lower.contains("super.money") || lower == "view more" || lower == "share receipt" ||
            lower == "details" || lower == "completed" || lower == "check balance" ||
            lower == "done" || lower == "close" || lower == "back" || lower == "repeat" ||
            lower == "split with friends" || lower == "send again" || lower == "view history"
        ) return false

        return trimmed.any { it.isLetter() }
    }

    /**
     * Resolves the note candidate according to the rule:
     * It cannot be a string containing series of number@letters.
     * In that case, check the next or the previous line of string for a meaningful word.
     */
    private fun resolveValidNote(
        lines: List<String>,
        candidateIndex: Int,
        labelIndex: Int,
        initialCandidate: String
    ): String {
        // If initial candidate is valid and NOT number@letters
        if (initialCandidate.isNotEmpty() && !isNumberAtLetters(initialCandidate) && isMeaningfulWord(initialCandidate)) {
            return initialCandidate
        }

        // If candidate contains number@letters, or is invalid:
        // Rule: "check the next or the previous line of string for a meaningful word"

        // 1. Check the next line (relative to candidateIndex or labelIndex)
        val nextIndices = listOf(candidateIndex + 1, labelIndex + 2, candidateIndex + 2)
            .filter { it in lines.indices && it != labelIndex && it != candidateIndex }
            .distinct()

        for (nextIdx in nextIndices) {
            val nextCandidate = lines[nextIdx]
            if (isMeaningfulWord(nextCandidate)) {
                return nextCandidate
            }
        }

        // 2. Check the previous line (relative to labelIndex or candidateIndex)
        val prevIndices = listOf(labelIndex - 1, candidateIndex - 1, labelIndex - 2)
            .filter { it in lines.indices && it != labelIndex && it != candidateIndex }
            .distinct()

        for (prevIdx in prevIndices) {
            val prevCandidate = lines[prevIdx]
            if (isMeaningfulWord(prevCandidate)) {
                return prevCandidate
            }
        }

        return ""
    }

    private fun findMeaningfulWordAdjacent(lines: List<String>, fromIndex: Int): String {
        // Check next line first
        if (fromIndex + 1 < lines.size && isMeaningfulWord(lines[fromIndex + 1])) {
            return lines[fromIndex + 1]
        }
        // Check previous line
        if (fromIndex - 1 >= 0 && isMeaningfulWord(lines[fromIndex - 1])) {
            return lines[fromIndex - 1]
        }
        // Check 2 lines after
        if (fromIndex + 2 < lines.size && isMeaningfulWord(lines[fromIndex + 2])) {
            return lines[fromIndex + 2]
        }
        // Check 2 lines before
        if (fromIndex - 2 >= 0 && isMeaningfulWord(lines[fromIndex - 2])) {
            return lines[fromIndex - 2]
        }
        return ""
    }

    private fun isKnownLabel(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower.startsWith("paid to") ||
                lower.startsWith("upi reference") ||
                lower.startsWith("upi transaction") ||
                lower.startsWith("payment method") ||
                lower.startsWith("date and time") ||
                lower.startsWith("super.money") ||
                lower.startsWith("view more") ||
                lower == "note" ||
                lower.startsWith("note:") ||
                lower == "remark" ||
                lower == "remarks" ||
                lower.startsWith("remark:") ||
                lower.startsWith("remarks:") ||
                lower == "details" ||
                lower.startsWith("transaction details") ||
                lower.startsWith("transfer details") ||
                lower.startsWith("completed") ||
                lower.startsWith("share receipt")
    }

    private fun extractVpa(text: String): String {
        val matcher = VPA_REGEX.matcher(text)
        return if (matcher.find()) matcher.group(0) ?: "" else ""
    }
}
