package pe.brice.smsreplay.data.model

/**
 * Data layer model for filter settings
 * Stored using DataStore
 */
data class FilterSettingsData(
    val senderNumber: String? = null, // Filter by sender phone number (optional)
    val bodyKeyword: String? = null  // Filter by SMS body keyword (optional)
) {
    /**
     * Check if SMS matches filter criteria
     * Both filters use AND condition: both must match if set
     */
    fun matches(sender: String, body: String): Boolean {
        // If no filters are set, accept all SMS
        if (senderNumber.isNullOrBlank() && bodyKeyword.isNullOrBlank()) {
            return true
        }

        // Check sender number filter (if set)
        val senderMatches = senderNumber.isNullOrBlank() ||
                sender.contains(senderNumber, ignoreCase = true)

        // Check body keyword filter (if set)
        val bodyMatches = bodyKeyword.isNullOrBlank() ||
                body.contains(bodyKeyword, ignoreCase = true)

        // AND condition: both must match
        return senderMatches && bodyMatches
    }

    /**
     * Check if any filters are active
     */
    fun hasActiveFilters(): Boolean {
        return !senderNumber.isNullOrBlank() || !bodyKeyword.isNullOrBlank()
    }

    /**
     * Get filter description for UI display
     */
    fun getDescription(): String {
        val parts = mutableListOf<String>()

        if (!senderNumber.isNullOrBlank()) {
            parts.add("발신자: $senderNumber")
        }

        if (!bodyKeyword.isNullOrBlank()) {
            parts.add("키워드: $bodyKeyword")
        }

        return if (parts.isEmpty()) {
            "모든 SMS 전송"
        } else {
            parts.joinToString(" AND ")
        }
    }
}
