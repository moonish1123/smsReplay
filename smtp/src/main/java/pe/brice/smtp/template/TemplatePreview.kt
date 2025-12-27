package pe.brice.smtp.template

/**
 * Utility for previewing email templates
 * For testing and development purposes
 */
object TemplatePreview {

    /**
     * Get sample HTML template for preview
     */
    fun getSampleTemplate(): String {
        return EmailTemplateBuilder.buildSmsTemplate(
            sender = "01012345678",
            body = "안녕하세요! 인증 번호는 [123456]입니다.\n\n이 인증 번호를 입력하여 본인 확인을 완료해 주세요.",
            timestamp = "2024-12-26 14:30",
            subject = "[FW SMS] 01012345678 (2024-12-26 14:30)"
        )
    }

    /**
     * Get test templates with various content
     */
    fun getTestTemplates(): List<Triple<String, String, String>> {
        return listOf(
            Triple(
                "01012345678",
                "간단한 메시지 테스트",
                "2024-12-26 09:00"
            ),
            Triple(
                "1588-0000",
                "[KB국민은행] 12월 26일 입금 1,000,000원\n잔액은 5,432,100원입니다.",
                "2024-12-26 10:15"
            ),
            Triple(
                "031-123-4567",
                "배송 알림\n\n고객님께서 주문하신 상품이 배송 시작되었습니다.\n\n배송예정일: 12월 28일\n배송기사: 김철수\n연락처: 010-9876-5432",
                "2024-12-26 14:45"
            ),
            Triple(
                "010-9876-5432",
                "A".repeat(200), // Long message test
                "2024-12-26 15:00"
            )
        )
    }
}
