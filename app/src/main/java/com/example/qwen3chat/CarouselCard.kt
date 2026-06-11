package com.example.qwen3chat

data class CarouselCard(
    val icon: String,
    val title: String,
    val description: String
) {
    companion object {
        val CARDS = listOf(
            CarouselCard(
                icon = "🔒",
                title = "Your Data Stays Private",
                description = "No data leaves your device. All conversations stay on your phone — no cloud, no tracking, no servers."
            ),
            CarouselCard(
                icon = "✨",
                title = "No Subscription Required",
                description = "This AI runs entirely on your device. Free forever — no monthly fees, no API costs."
            ),
            CarouselCard(
                icon = "🔧",
                title = "Fully Customizable",
                description = "Open-source means you're in control. Modify, extend, or integrate — the code is yours."
            ),
            CarouselCard(
                icon = "📡",
                title = "Works Offline",
                description = "No internet needed after download. Chat anywhere — planes, trains, or off-grid."
            ),
            CarouselCard(
                icon = "🧠",
                title = "Reasoning Model",
                description = "Powered by Qwen3.5 with chain-of-thought reasoning. Watch the AI think through problems step by step."
            )
        )
    }
}
