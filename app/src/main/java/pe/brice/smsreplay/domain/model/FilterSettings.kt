package pe.brice.smsreplay.domain.model

/**
 * Domain model for filter settings
 */
data class FilterSettings(
    val senderNumber: String? = null,
    val bodyKeyword: String? = null
) {
    /**
     * Check if SMS matches filter criteria
     * AND condition: both filters must match if set
     */
    fun matches(sender: String, body: String): Boolean {
        // If no filters, accept all
        if (senderNumber.isNullOrBlank() && bodyKeyword.isNullOrBlank()) {
            return true
        }

        val senderMatches = senderNumber.isNullOrBlank() ||
                sender.contains(senderNumber, ignoreCase = true)

        val bodyMatches = bodyKeyword.isNullOrBlank() ||
                body.contains(bodyKeyword, ignoreCase = true)

        return senderMatches && bodyMatches
    }

    fun hasActiveFilters(): Boolean {
        return !senderNumber.isNullOrBlank() || !bodyKeyword.isNullOrBlank()
    }
}
