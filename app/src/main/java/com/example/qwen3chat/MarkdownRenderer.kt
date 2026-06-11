package com.example.qwen3chat

import android.text.Spanned
import androidx.core.text.HtmlCompat

object MarkdownRenderer {

    fun render(markdown: String): Spanned {
        var html = markdown
            // Code blocks (```...```)
            .replace(Regex("```(\\w*)\\n([\\s\\S]*?)```")) { match ->
                val code = match.groupValues[2]
                "<br><tt><pre>$code</pre></tt><br>"
            }
            // Inline code (`code`)
            .replace(Regex("`([^`]+)`")) { match ->
                "<tt>${match.groupValues[1]}</tt>"
            }
            // Bold (**text**)
            .replace(Regex("\\*\\*(.+?)\\*\\*")) { match ->
                "<b>${match.groupValues[1]}</b>"
            }
            // Italic (*text*)
            .replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")) { match ->
                "<i>${match.groupValues[1]}</i>"
            }
            // Unordered lists (- item)
            .replace(Regex("(?m)^- (.+)$")) { match ->
                "• ${match.groupValues[1]}<br>"
            }
            // Ordered lists (1. item)
            .replace(Regex("(?m)^\\d+\\. (.+)$")) { match ->
                "${match.groupValues[1]}<br>"
            }
            // Headers (### text)
            .replace(Regex("(?m)^### (.+)$")) { match ->
                "<b>${match.groupValues[1]}</b><br>"
            }
            .replace(Regex("(?m)^## (.+)$")) { match ->
                "<b><big>${match.groupValues[1]}</big></b><br>"
            }
            .replace(Regex("(?m)^# (.+)$")) { match ->
                "<b><big><big>${match.groupValues[1]}</big></big></b><br>"
            }
            // Line breaks
            .replace("\n", "<br>")

        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}
