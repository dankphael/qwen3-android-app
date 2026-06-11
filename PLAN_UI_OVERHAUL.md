# PLAN_UI_OVERHAUL.md — Qwen3 Chat App UI/UX Overhaul

**Author:** Pat (AI Technical Team Lead)
**Date:** 2026-06-08
**Executor:** Suresh (AI Agent 2)

---

## Overview

Complete UI/UX overhaul of the qwen3-android-app per DESIGN_SPEC.md (Dark Theme palette, Light Theme palette, typography, spacing, animations). Five self-contained phases. Each phase ends with a visual test checkpoint.

**Key architectural decisions:**
- All XML layouts remain View-based (not Compose) — matches existing codebase
- Theme is Material3 DayNight with programmatic color overrides where XML themes can't express the full token set
- Transition animations use AndroidX Transition library + Material motion
- Typography applied via XML TextAppearance styles (not Compose TextStyle)

---

## Phase 1: Theme Foundation

**Goal:** Correct colors, dimensions, and typography tokens available to all screens. Nothing visual changes yet — this is infrastructure only.

**Dependencies:** None (first phase)

### Files to CREATE

| # | File | Purpose |
|---|------|---------|
| 1 | `app/src/main/res/values/colors.xml` | Complete rewrite: all DESIGN_SPEC color tokens |
| 2 | `app/src/main/res/values/dimens.xml` | Spacing scale, radii, icon sizes, component heights |
| 3 | `app/src/main/res/values/typography.xml` | TextAppearance styles for every DESIGN_SPEC type scale entry |

### Files to MODIFY

| # | File | Change |
|---|------|--------|
| 4 | `app/src/main/res/values/themes.xml` | Rewrite: light theme referencing DESIGN_SPEC light palette tokens |
| 5 | `app/src/main/res/values-night/themes.xml` | Rewrite: dark theme referencing DESIGN_SPEC dark palette tokens |

### Specific Changes

#### 1. colors.xml (full rewrite)

Replace all legacy `purple_200/500/700` entries. Every DESIGN_SPEC token gets an Android color resource:

```
<!-- Dark theme tokens (default for values-night override) -->
<color name="background">#0D0D0D</color>
<color name="surface">#1A1A1A</color>
<color name="surface_variant">#262626</color>
<color name="primary">#7C6FF7</color>
<color name="primary_container">#2D2566</color>
<color name="on_primary">#FFFFFF</color>
<color name="on_primary_container">#E0DBFF</color>
<color name="secondary">#FFB74D</color>
<color name="secondary_container">#3E2D14</color>
<color name="on_secondary">#1A0D00</color>
<color name="on_secondary_container">#FFE0B2</color>
<color name="error">#FF5252</color>
<color name="on_error">#FFFFFF</color>
<color name="on_surface">#EDEDED</color>
<color name="on_surface_variant">#B0B0B0</color>
<color name="outline">#404040</color>
<color name="outline_variant">#2E2E2E</color>
<color name="inverse_surface">#F0F0F0</color>
<color name="inverse_on_surface">#1A1A1A</color>

<!-- Code block background -->
<color name="code_bg_dark">#0D0D0D</color>
<color name="code_bg_light">#F5F5F5</color>

<!-- Download complete -->
<color name="download_complete">#4CAF50</color>

<!-- Toolbar surface (92% alpha of surface) -->
<color name="toolbar_surface">#E6FFFFFF</color>  <!-- actually calculated at runtime -->
```

The light theme palette overrides go in `values/themes.xml` color references — or better: put dark tokens in values-night/colors.xml, light tokens in values/colors.xml. Split approach:

**values/colors.xml** — light palette:
```
<color name="background">#FAFAFA</color>
<color name="surface">#FFFFFF</color>
<color name="surface_variant">#F0EEFC</color>
<color name="primary">#6C5CE7</color>
<color name="primary_container">#E8E5FF</color>
<color name="on_primary">#FFFFFF</color>
<color name="on_primary_container">#1E1666</color>
<color name="secondary">#F09B2C</color>
<color name="secondary_container">#FFF3E0</color>
<color name="on_secondary">#FFFFFF</color>
<color name="on_secondary_container">#4E2D00</color>
<color name="error">#E53935</color>
<color name="on_error">#FFFFFF</color>
<color name="on_surface">#1A1A1A</color>
<color name="on_surface_variant">#5C5C5C</color>
<color name="outline">#CBCBCB</color>
<color name="outline_variant">#E8E8E8</color>
<color name="inverse_surface">#2A2A2A</color>
<color name="inverse_on_surface">#F5F5F5</color>
<color name="code_bg_light">#F5F5F5</color>
<color name="code_bg_dark">#0D0D0D</color>
<color name="download_complete">#4CAF50</color>
```

**values-night/colors.xml** — dark palette (same name keys, different hex values):
```
<color name="background">#0D0D0D</color>
<color name="surface">#1A1A1A</color>
<color name="surface_variant">#262626</color>
<color name="primary">#7C6FF7</color>
...same structure, dark hex values from spec
```

#### 2. dimens.xml

```
<dimen name="base_unit">4dp</dimen>
<dimen name="space_0">0dp</dimen>
<dimen name="space_2">2dp</dimen>
<dimen name="space_4">4dp</dimen>
<dimen name="space_6">6dp</dimen>
<dimen name="space_8">8dp</dimen>
<dimen name="space_12">12dp</dimen>
<dimen name="space_16">16dp</dimen>
<dimen name="space_20">20dp</dimen>
<dimen name="space_24">24dp</dimen>
<dimen name="space_32">32dp</dimen>
<dimen name="space_40">40dp</dimen>
<dimen name="space_48">48dp</dimen>
<dimen name="space_56">56dp</dimen>
<dimen name="space_64">64dp</dimen>

<dimen name="screen_h_padding">16dp</dimen>
<dimen name="card_padding">16dp</dimen>
<dimen name="list_item_h_padding">12dp</dimen>
<dimen name="list_item_v_padding">8dp</dimen>
<dimen name="section_gap">24dp</dimen>

<dimen name="toolbar_height">56dp</dimen>
<dimen name="toolbar_icon_size">24dp</dimen>
<dimen name="toolbar_icon_margin">12dp</dimen>
<dimen name="toolbar_tap_target">40dp</dimen>
<dimen name="toolbar_icon_gap">4dp</dimen>

<dimen name="bubble_corner_radius">20dp</dimen>
<dimen name="bubble_padding_h">16dp</dimen>
<dimen name="bubble_padding_v">12dp</dimen>
<dimen name="bubble_margin_top">2dp</dimen>
<dimen name="bubble_margin_bottom">2dp</dimen>
<dimen name="bubble_margin_start">16dp</dimen>
<dimen name="bubble_margin_end">16dp</dimen>
<dimen name="bubble_max_width_user">0.80</dimen>  <!-- fraction, used programmatically -->
<dimen name="bubble_margin_start_user">64dp</dimen>
<dimen name="bubble_margin_end_assistant">64dp</dimen>

<dimen name="code_block_radius">12dp</dimen>
<dimen name="code_block_padding">12dp</dimen>
<dimen name="code_copy_button_size">28dp</dimen>
<dimen name="code_copy_inset">8dp</dimen>

<dimen name="thinking_collapsed_height">36dp</dimen>
<dimen name="thinking_expanded_max_height">300dp</dimen>
<dimen name="thinking_radius">12dp</dimen>
<dimen name="thinking_padding_h">12dp</dimen>
<dimen name="thinking_padding_v">10dp</dimen>
<dimen name="thinking_icon_size">18dp</dimen>
<dimen name="thinking_chevron_size">16dp</dimen>

<dimen name="input_bar_min_height">56dp</dimen>
<dimen name="input_bar_max_height">120dp</dimen>
<dimen name="input_bar_radius">28dp</dimen>
<dimen name="input_bar_padding">4dp</dimen>
<dimen name="input_bar_margin_h">12dp</dimen>
<dimen name="input_bar_margin_v">8dp</dimen>
<dimen name="send_button_size">40dp</dimen>
<dimen name="send_button_icon_size">24dp</dimen>
<dimen name="attach_button_size">40dp</dimen>

<dimen name="typing_indicator_height">36dp</dimen>
<dimen name="typing_indicator_radius">18dp</dimen>
<dimen name="typing_dot_size">8dp</dimen>
<dimen name="typing_dot_gap">6dp</dimen>

<dimen name="download_card_height">200dp</dimen>
<dimen name="download_card_radius">20dp</dimen>
<dimen name="download_card_padding">20dp</dimen>
<dimen name="download_icon_size">48dp</dimen>
<dimen name="download_progress_height">6dp</dimen>
<dimen name="download_progress_radius">3dp</dimen>
<dimen name="download_button_height">56dp</dimen>
<dimen name="download_button_radius">28dp</dimen>

<dimen name="welcome_icon_size">80dp</dimen>
<dimen name="welcome_padding">32dp</dimen>
<dimen name="suggestion_chip_radius">20dp</dimen>
<dimen name="suggestion_chip_padding_h">16dp</dimen>
<dimen name="suggestion_chip_padding_v">10dp</dimen>
```

#### 3. typography.xml

Create TextAppearance styles for each type scale entry:

```
<style name="TextAppearance.Qwen.DisplayLarge" parent="TextAppearance.Material3.DisplayLarge">
    <item name="fontFamily">@font/inter_bold</item>  <!-- requires adding Inter font -->
    <item name="android:textSize">57sp</item>
    <item name="android:lineHeight">64sp</item>
    <item name="android:letterSpacing">-0.25</item>
</style>
```

Same pattern for all 15 type styles from DESIGN_SPEC §3. Two approaches for Inter font:

**Option A (recommended):** Add Inter .ttf files to `app/src/main/res/font/` and reference via `@font/inter_regular`, `@font/inter_medium`, `@font/inter_semibold`, `@font/inter_bold`. This avoids downloadable fonts lag on first paint.

**Option B:** Use downloadable fonts via `fontFamily="@font/inter"` with a font provider in XML — simpler but has FOUT.

For Phase 1, create the TextAppearance styles as scaffolding. Actual font files can be added in a sub-step or Phase 2.

Also add monospace style:
```
<style name="TextAppearance.Qwen.Code" parent="TextAppearance.Material3.BodyMedium">
    <item name="fontFamily">@font/jetbrains_mono_regular</item>
    <item name="android:textSize">14sp</item>
    <item name="android:lineHeight">20sp</item>
    <item name="android:letterSpacing">0.00</item>
</style>
```

#### 4. themes.xml (light — values/themes.xml)

Rewrite. Core approach: Material3 DayNight with custom color attributes:

```xml
<style name="Theme.Qwen3Chat" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- Primary colors -->
    <item name="colorPrimary">@color/primary</item>
    <item name="colorOnPrimary">@color/on_primary</item>
    <item name="colorPrimaryContainer">@color/primary_container</item>
    <item name="colorOnPrimaryContainer">@color/on_primary_container</item>

    <!-- Secondary colors -->
    <item name="colorSecondary">@color/secondary</item>
    <item name="colorOnSecondary">@color/on_secondary</item>
    <item name="colorSecondaryContainer">@color/secondary_container</item>
    <item name="colorOnSecondaryContainer">@color/on_secondary_container</item>

    <!-- Error -->
    <item name="colorError">@color/error</item>
    <item name="colorOnError">@color/on_error</item>

    <!-- Surface/Background -->
    <item name="android:colorBackground">@color/background</item>
    <item name="colorSurface">@color/surface</item>
    <item name="colorOnSurface">@color/on_surface</item>
    <item name="colorSurfaceVariant">@color/surface_variant</item>
    <item name="colorOnSurfaceVariant">@color/on_surface_variant</item>

    <!-- Outline -->
    <item name="colorOutline">@color/outline</item>
    <item name="colorOutlineVariant">@color/outline_variant</item>

    <!-- Status bar -->
    <item name="android:statusBarColor">@color/background</item>
    <item name="android:navigationBarColor">@color/background</item>
    <item name="android:windowLightStatusBar">true</item>
</style>
```

#### 5. themes.xml (dark — values-night/themes.xml)

Same structure but `android:windowLightStatusBar` is `false`.

### Gotchas — Phase 1

- **Material3 attribute names:** `colorSurfaceVariant` and `colorOutlineVariant` are Material3-only. Verify the parent theme supports them. If using `Theme.Material3.DayNight.NoActionBar`, they work. If `Theme.MaterialComponents.DayNight.NoActionBar` (MDC), use `?attr/colorSurfaceVariant` in XML but the attribute may need explicit definition.
- **Inter font files:** If font files aren't bundled yet, TextAppearance styles will fall back to sans-serif. That's acceptable for Phase 1 — add fonts in Phase 2 when they're first visible.
- **`values-night/colors.xml`:** Android's resource system automatically picks `values-night/` for dark mode. Both `values/colors.xml` and `values-night/colors.xml` must define the SAME set of `<color name="...">` keys with different hex values.
- **Toolbar surface 92% alpha:** Cannot express as a static color resource. Will be handled programmatically in Phase 3 with `ColorUtils.setAlphaComponent()`.

### Test Checkpoint — Phase 1

1. Build project — no resource compilation errors
2. Toggle between light/dark system theme — verify background/surface/primary colors switch correctly
3. Inspect with Layout Inspector: `@color/primary` resolves to `#6C5CE7` (light) and `#7C6FF7` (dark)
4. All 19 color tokens are defined and non-null in both themes

---

## Phase 2: Message Bubbles & Thinking Section

**Goal:** ChatAdapter displays correctly styled message bubbles (20dp corners, H16V12 padding, maxWidth constraints, timestamps below), and the thinking section has expanding/collapsing animation.

**Dependencies:** Phase 1 (colors, dimens, typography)

### Files to CREATE

| # | File | Purpose |
|---|------|---------|
| 1 | `app/src/main/res/drawable/bg_bubble_user.xml` | User bubble shape: @color/primary fill, @dimen/bubble_corner_radius (20dp) all corners |
| 2 | `app/src/main/res/drawable/bg_bubble_assistant.xml` | Assistant bubble shape: @color/surface_variant fill, 20dp all corners |
| 3 | `app/src/main/res/drawable/bg_code_block.xml` | Code block shape: @color/code_bg_dark fill, @dimen/code_block_radius (12dp) |
| 4 | `app/src/main/res/drawable/bg_thinking_expanded.xml` | Thinking expanded bg: @color/primary_container at 40% alpha (use solid color, apply alpha in code), 12dp radius |
| 5 | `app/src/main/res/anim/thinking_expand.xml` | Expand animation: height 0→match, content alpha 0→1, 300ms FastOutSlowIn |
| 6 | `app/src/main/res/anim/thinking_collapse.xml` | Collapse animation: height match→0, content alpha 1→0, 250ms FastOutSlowIn |
| 7 | `app/src/main/res/anim/message_appear.xml` | Message appear: translateY 24dp→0 + alpha 0→1, 250ms |
| 8 | `app/src/main/res/anim/chevron_rotate_cw.xml` | Chevron rotate 0→180°, 300ms |
| 9 | `app/src/main/res/anim/chevron_rotate_ccw.xml` | Chevron rotate 180→0°, 250ms |

### Files to MODIFY

| # | File | Change |
|---|------|--------|
| 10 | `app/src/main/res/layout/item_message.xml` | Complete restructure: new bubble layout, timestamps below bubble, thinking section, code block sections |
| 11 | `app/src/main/java/com/example/qwen3chat/ChatAdapter.kt` | Major rewrite: multiple ViewTypes (USER, ASSISTANT, ASSISTANT_THINKING, TYPING), maxWidth constraints, animation on bind, timestamp formatting |
| 12 | `app/src/main/java/com/example/qwen3chat/ChatMessage.kt` | Add `hasThinking`, `thinkingContent`, `thinkingExpanded`, `codeBlocks` fields |
| 13 | `app/src/main/java/com/example/qwen3chat/ReasoningParser.kt` | Update to parse thinking tags (`<thinking>...</thinking>`) and code blocks (``` fences), return structured ChatMessage |
| 14 | `app/src/main/res/drawable/bg_message_user.xml` | DELETE (replaced by bg_bubble_user) |
| 15 | `app/src/main/res/drawable/bg_message_bot.xml` | DELETE (replaced by bg_bubble_assistant) |

### Detailed Changes

#### item_message.xml (full restructure)

The layout needs to support multiple ViewTypes via visibility toggling, OR via separate layouts. **Decision:** Use single layout with visibility toggling on sections (simpler for now; separate ViewTypes in Adapter for Phase 2).

New structure:
```xml
<ConstraintLayout android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingHorizontal="@dimen/screen_h_padding">

    <!-- ── THINKING SECTION ── -->
    <LinearLayout android:id="@+id/thinkingSection"
        android:visibility="gone"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="@+id/bubbleContainer"
        app:layout_constraintEnd_toEndOf="@+id/bubbleContainer">

        <!-- Collapsed header: always visible -->
        <LinearLayout android:id="@+id/thinkingHeader"
            android:layout_width="match_parent"
            android:layout_height="@dimen/thinking_collapsed_height"
            android:gravity="center_vertical"
            android:paddingHorizontal="0dp">

            <!--  brain emoji icon  -->
            <ImageView android:id="@+id/thinkingIcon"
                android:layout_width="@dimen/thinking_icon_size"
                android:layout_height="@dimen/thinking_icon_size"
                android:src="@drawable/ic_brain"   <!-- vector drawable -->
                android:contentDescription="Thinking" />

            <TextView android:id="@+id/thinkingLabel"
                android:layout_width="0dp" android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Thinking"
                android:textAppearance="@style/TextAppearance.Qwen.LabelMedium"
                android:textColor="@color/secondary" />

            <ImageView android:id="@+id/thinkingChevron"
                android:layout_width="@dimen/thinking_chevron_size"
                android:layout_height="@dimen/thinking_chevron_size"
                android:src="@drawable/ic_chevron_down"
                android:rotation="0" />
        </LinearLayout>

        <!-- Expanded content: visibility toggled -->
        <LinearLayout android:id="@+id/thinkingContent"
            android:visibility="gone"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:maxHeight="@dimen/thinking_expanded_max_height"
            android:background="@drawable/bg_thinking_expanded"
            android:padding="@dimen/thinking_padding_h">

            <TextView android:id="@+id/thinkingText"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textAppearance="@style/TextAppearance.Qwen.BodySmall"
                android:textColor="@color/on_primary_container"
                android:alpha="0.8" />
        </LinearLayout>
    </LinearLayout>

    <!-- ── MESSAGE BUBBLE ── -->
    <FrameLayout android:id="@+id/bubbleContainer"
        android:layout_width="0dp" android:layout_height="wrap_content"
        app:layout_constraintTop_toBottomOf="@id/thinkingSection"
        ...>

        <LinearLayout android:id="@+id/bubble"
            android:orientation="vertical"
            android:background="@drawable/bg_bubble_user"
            android:paddingHorizontal="@dimen/bubble_padding_h"
            android:paddingVertical="@dimen/bubble_padding_v">

            <TextView android:id="@+id/tvMessage"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textAppearance="@style/TextAppearance.Qwen.BodyLarge"
                .../>

            <!-- Code block placeholder (generated programmatically) -->
            <FrameLayout android:id="@+id/codeBlockContainer"
                android:visibility="gone" .../>
        </LinearLayout>
    </FrameLayout>

    <!-- ── TIMESTAMP ── -->
    <TextView android:id="@+id/tvTimestamp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textAppearance="@style/TextAppearance.Qwen.LabelSmall"
        android:textColor="@color/on_surface_variant"
        android:layout_marginTop="@dimen/space_4"
        app:layout_constraintTop_toBottomOf="@id/bubbleContainer"
        .../>
</ConstraintLayout>
```

#### ChatAdapter.kt (major rewrite)

Key changes:

1. **ViewHolder with all view references:**
   ```kotlin
   class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
       val thinkingSection: LinearLayout = view.findViewById(R.id.thinkingSection)
       val thinkingHeader: LinearLayout = view.findViewById(R.id.thinkingHeader)
       val thinkingContent: LinearLayout = view.findViewById(R.id.thinkingContent)
       val thinkingText: TextView = view.findViewById(R.id.thinkingText)
       val thinkingChevron: ImageView = view.findViewById(R.id.thinkingChevron)
       val bubbleContainer: FrameLayout = view.findViewById(R.id.bubbleContainer)
       val bubble: LinearLayout = view.findViewById(R.id.bubble)
       val tvMessage: TextView = view.findViewById(R.id.tvMessage)
       val codeBlockContainer: FrameLayout = view.findViewById(R.id.codeBlockContainer)
       val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
   }
   ```

2. **onBindViewHolder — maxWidth constraint:**
   ```kotlin
   val displayMetrics = context.resources.displayMetrics
   val screenWidth = displayMetrics.widthPixels
   val maxWidth = if (message.isUser) (screenWidth * 0.80).toInt()
                  else (screenWidth * 0.88).toInt()
   bubbleContainer.layoutParams = (bubbleContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
       matchConstraintMaxWidth = maxWidth
   }
   ```

3. **onBindViewHolder — alignment:**
   ```kotlin
   // For user: constrain to end
   (bubbleContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
       endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
       startToStart = ConstraintLayout.LayoutParams.UNSET
       marginStart = resources.getDimensionPixelSize(R.dimen.bubble_margin_start_user)
   }
   // For assistant: constrain to start
   (bubbleContainer.layoutParams as ConstraintLayout.LayoutParams).apply {
       startToStart = ConstraintLayout.LayoutParams.PARENT_ID
       endToEnd = ConstraintLayout.LayoutParams.UNSET
       marginEnd = resources.getDimensionPixelSize(R.dimen.bubble_margin_end_assistant)
   }
   ```

4. **Bubble background swap:**
   ```kotlin
   holder.bubble.background = if (message.isUser) {
       AppCompatResources.getDrawable(context, R.drawable.bg_bubble_user)
   } else {
       AppCompatResources.getDrawable(context, R.drawable.bg_bubble_assistant)
   }
   ```

5. **Text color swap:**
   ```kotlin
   holder.tvMessage.setTextColor(
       if (message.isUser) ContextCompat.getColor(context, R.color.on_primary)
       else ContextCompat.getColor(context, R.color.on_surface)
   )
   ```

6. **Thinking section bind:**
   ```kotlin
   if (message.hasThinking) {
       holder.thinkingSection.visibility = View.VISIBLE
       holder.thinkingText.text = message.thinkingContent
       holder.thinkingContent.visibility = if (message.thinkingExpanded) View.VISIBLE else View.GONE
       holder.thinkingChevron.rotation = if (message.thinkingExpanded) 180f else 0f
       holder.thinkingHeader.setOnClickListener {
           message.thinkingExpanded = !message.thinkingExpanded
           notifyItemChanged(position)
       }
   } else {
       holder.thinkingSection.visibility = View.GONE
   }
   ```

7. **Code blocks:** For simplicity in Phase 2, render code blocks as styled TextViews within the bubble rather than complex FrameLayout copy-button structure. Full code block UI can iterate in Phase 3.

8. **Timestamps:**
   ```kotlin
   holder.tvTimestamp.text = SimpleDateFormat("HH:mm", Locale.getDefault())
       .format(Date(message.timestamp))
   // Align based on user/assistant
   (holder.tvTimestamp.layoutParams as ConstraintLayout.LayoutParams).apply {
       if (message.isUser) {
           endToEnd = R.id.bubbleContainer
           startToStart = ConstraintLayout.LayoutParams.UNSET
       } else {
           startToStart = R.id.bubbleContainer
           endToEnd = ConstraintLayout.LayoutParams.UNSET
       }
   }
   ```

9. **Message appear animation (on bind):**
   ```kotlin
   // Only animate if this is a newly added message (track via adapter position vs lastKnownSize)
   // Use ObjectAnimator or TransitionManager
   holder.itemView.alpha = 0f
   holder.itemView.translationY = 24f
   holder.itemView.animate()
       .alpha(1f)
       .translationY(0f)
       .setDuration(250)
       .setInterpolator(FastOutSlowInInterpolator())
       .start()
   ```

#### ChatMessage.kt (add fields)

```kotlin
data class ChatMessage(
    val id: Long,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long,
    // NEW FIELDS:
    val hasThinking: Boolean = false,
    val thinkingContent: String = "",
    var thinkingExpanded: Boolean = false,
    val codeBlocks: List<CodeBlock> = emptyList()
)

data class CodeBlock(
    val language: String,
    val code: String
)
```

#### ReasoningParser.kt (update)

Parse `...` (qwen3 format) or `<thinking>...</thinking>` tags. Split content into thinking + answer + code blocks. Return ChatMessage with all fields populated.

### Gotchas — Phase 2

- **ConstraintLayout.LayoutParams cast:** `item_message.xml` root must be ConstraintLayout for the maxWidth constraint trick to work.
- **maxWidth on FrameLayout:** `matchConstraintMaxWidth` only works with `layout_width="0dp"` (match_constraint). Ensure bubbleContainer uses `0dp` width.
- **Animation on bind — recycler ghosting:** When ViewHolders are recycled, the animation fires again on old items. Guard with a set of "already animated" positions or use `ItemAnimator`.
- **Thinking expanded state:** Stored in ChatMessage.thinkingExpanded (mutable). On `notifyItemChanged(position)`, the ViewHolder rebinds with new visibility. The expand/collapse animation needs `TransitionManager.beginDelayedTransition()` or a ValueAnimator on the LinearLayout height.
- **Code block background color:** Must be programmatic since light/dark switch: light=`#F5F5F5`, dark=`#0D0D0D`. Use `AppCompatResources.getColorStateList()` or check night mode at bind time.
- **`bg_message_user.xml` and `bg_message_bot.xml`:** Delete only AFTER confirming no other code references them. Search the entire project first.

### Test Checkpoint — Phase 2

1. Send a message — bubble appears with 20dp rounded corners, purple background, white text, aligned right
2. Send an assistant message (mock) — bubble with gray surfaceVariant background, dark text, aligned left
3. Timestamps show "HH:mm" format below bubbles, correctly aligned left/right
4. Message appear animation: fade in + slight slide up
5. Thinking section: collapsed (36dp bar with "Thinking" label), tap expands to show content with 12dp radius background, tap again collapses
6. User bubble maxWidth = 80% of screen, assistant = 88%
7. Spacing: 4dp between consecutive messages, 16dp between date groups

---

## Phase 3: Input Bar & Empty State

**Goal:** Pill-shaped input bar with attach button, send button morphing states (mic → send → stop), typing indicator in message list, empty state with welcome text and suggestion chips.

**Dependencies:** Phase 1 (colors, dimens), Phase 2 (ChatAdapter must support TYPING ViewType)

### Files to CREATE

| # | File | Purpose |
|---|------|---------|
| 1 | `app/src/main/res/drawable/bg_input_bar.xml` | Pill shape (28dp radius), surfaceVariant fill, 2dp elevation shadow |
| 2 | `app/src/main/res/drawable/bg_send_button_idle.xml` | Circle (20dp radius), transparent |
| 3 | `app/src/main/res/drawable/bg_send_button_ready.xml` | Circle, primary fill |
| 4 | `app/src/main/res/drawable/bg_send_button_sending.xml` | Circle, error (#FF5252) fill |
| 5 | `app/src/main/res/drawable/bg_chip_suggestion.xml` | Pill (20dp radius), surfaceVariant fill, 1dp outlineVariant stroke |
| 6 | `app/src/main/res/drawable/ic_mic.xml` | Vector: material mic icon |
| 7 | `app/src/main/res/drawable/ic_send.xml` | Vector: arrow_upward |
| 8 | `app/src/main/res/drawable/ic_stop.xml` | Vector: square stop icon |
| 9 | `app/src/main/res/drawable/ic_attach.xml` | Vector: add_circle_outline |
| 10 | `app/src/main/res/layout/view_typing_indicator.xml` | 3-dot typing animation layout |
| 11 | `app/src/main/res/layout/view_empty_state.xml` | Welcome icon, text, suggestion chips |
| 12 | `app/src/main/res/anim/dot_pulse.xml` | ScaleY animation for typing dots (1.0→0.4→1.0, 200ms stagger) |

### Files to MODIFY

| # | File | Change |
|---|------|--------|
| 13 | `app/src/main/res/layout/fragment_chat_detail.xml` | Complete restructure: toolbar specs, new input bar, embed empty state and typing indicator |
| 14 | `app/src/main/java/com/example/qwen3chat/ui/chatdetail/ChatDetailFragment.kt` | Send button state machine (IDLE/READY/SENDING), text change listener, empty state toggle, typing indicator integration |
| 15 | `app/src/main/java/com/example/qwen3chat/ChatAdapter.kt` | Add TYPING ViewType (typing indicator as a sentinel item at position 0 or last when assistant is generating) |
| 16 | `app/src/main/java/com/example/qwen3chat/ui/chatdetail/ChatDetailViewModel.kt` | Add `isTyping` StateFlow, empty state logic |

### Detailed Changes

#### fragment_chat_detail.xml (restructure)

```xml
<androidx.constraintlayout.widget.ConstraintLayout ...>

    <!-- TOOLBAR -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="0dp"
        android:layout_height="@dimen/toolbar_height"
        android:paddingHorizontal="@dimen/screen_h_padding"
        android:background="@color/toolbar_surface"
        app:layout_constraintTop_toTopOf="parent"
        ...>
        <!-- Menu icon (24×24, 12dp marginEnd) -->
        <!-- Title: titleLarge (22sp), Medium(500), centeredVertically -->
        <!-- 3 right icons × 40dp tap target, 4dp gap -->
    </com.google.android.material.appbar.MaterialToolbar>

    <!-- MESSAGE LIST (with empty state overlay) -->
    <FrameLayout android:id="@+id/messageContainer"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/inputBar"
        ...>

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recyclerView"
            android:paddingTop="@dimen/space_8"
            android:paddingBottom="@dimen/space_16"
            .../>

        <!-- Empty state (overlaid when messages is empty) -->
        <include android:id="@+id/emptyState"
            layout="@layout/view_empty_state"
            android:visibility="gone" />
    </FrameLayout>

    <!-- INPUT BAR -->
    <LinearLayout android:id="@+id/inputBar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:minHeight="@dimen/input_bar_min_height"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:background="@drawable/bg_input_bar"
        android:padding="@dimen/input_bar_padding"
        android:layout_marginHorizontal="@dimen/input_bar_margin_h"
        android:layout_marginVertical="@dimen/input_bar_margin_v"
        app:layout_constraintBottom_toBottomOf="parent"
        ...>

        <!-- Attach button -->
        <ImageButton android:id="@+id/btnAttach"
            android:layout_width="@dimen/attach_button_size"
            android:layout_height="@dimen/attach_button_size"
            android:src="@drawable/ic_attach"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="Attach file" />

        <!-- Text field -->
        <EditText android:id="@+id/etInput"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:minHeight="40dp"
            android:maxLines="5"
            android:paddingHorizontal="@dimen/space_8"
            android:paddingVertical="@dimen/space_8"
            android:textAppearance="@style/TextAppearance.Qwen.BodyLarge"
            android:hint="Ask anything..."
            android:background="@null"
            android:inputType="textMultiLine|textCapSentences" />

        <!-- Send button -->
        <ImageButton android:id="@+id/btnSend"
            android:layout_width="@dimen/send_button_size"
            android:layout_height="@dimen/send_button_size"
            android:src="@drawable/ic_mic"
            android:background="@drawable/bg_send_button_idle"
            android:contentDescription="Send message" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

**Divider above input bar:** Add a 0.5dp View with `@color/outline_variant` background between messageContainer and inputBar.

#### ChatDetailFragment.kt (send button state machine)

```kotlin
enum class SendButtonState { IDLE, READY, SENDING }

private var sendState = SendButtonState.IDLE

// On text change listener
binding.etInput.addTextChangedListener(object : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        val hasText = !s.isNullOrBlank()
        if (sendState != SendButtonState.SENDING) {
            updateSendButton(if (hasText) SendButtonState.READY else SendButtonState.IDLE)
        }
    }
})

private fun updateSendButton(state: SendButtonState) {
    sendState = state
    val (icon, bg) = when (state) {
        SendButtonState.IDLE -> R.drawable.ic_mic to R.drawable.bg_send_button_idle
        SendButtonState.READY -> R.drawable.ic_send to R.drawable.bg_send_button_ready
        SendButtonState.SENDING -> R.drawable.ic_stop to R.drawable.bg_send_button_sending
    }
    // Animate the transition
    binding.btnSend.apply {
        animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction {
            setImageResource(icon)
            setBackgroundResource(bg)
            animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }
}

// On send click
binding.btnSend.setOnClickListener {
    when (sendState) {
        SendButtonState.IDLE -> { /* TODO: voice input */ }
        SendButtonState.READY -> {
            val text = binding.etInput.text.toString().trim()
            if (text.isNotEmpty()) {
                updateSendButton(SendButtonState.SENDING)
                viewModel.sendMessage(text)
                binding.etInput.text.clear()
            }
        }
        SendButtonState.SENDING -> {
            viewModel.stopGeneration()
            updateSendButton(SendButtonState.IDLE)
        }
    }
}

// Observe isTyping to reset button state
viewModel.isTyping.observe(viewLifecycleOwner) { typing ->
    if (!typing && sendState == SendButtonState.SENDING) {
        updateSendButton(SendButtonState.IDLE)
    }
}
```

#### view_empty_state.xml

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="@dimen/welcome_padding">

    <ImageView
        android:layout_width="@dimen/welcome_icon_size"
        android:layout_height="@dimen/welcome_icon_size"
        android:src="@mipmap/ic_launcher"
        android:alpha="0.8" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="What can I help with?"
        android:textAppearance="@style/TextAppearance.Qwen.HeadlineMedium"
        android:textAlignment="center"
        android:layout_marginTop="@dimen/space_24" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Ask anything — brainstorm, write, code, or just chat"
        android:textAppearance="@style/TextAppearance.Qwen.BodyMedium"
        android:textColor="@color/on_surface_variant"
        android:textAlignment="center"
        android:layout_marginTop="@dimen/space_8" />

    <com.google.android.flexbox.FlexboxLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:flexWrap="wrap"
        app:justifyContent="center"
        android:layout_marginTop="@dimen/space_24">

        <!-- 4 suggestion chips -->
        <TextView style="@style/Chip.Suggestion"
            android:text="☀️ Summarize this article"
            android:onClick="onSuggestionClick" />
        <TextView style="@style/Chip.Suggestion"
            android:text="🎨 Create a workout plan" />
        <TextView style="@style/Chip.Suggestion"
            android:text="💻 Write an email reply" />
        <TextView style="@style/Chip.Suggestion"
            android:text="🍳 Plan a dinner menu" />
    </com.google.android.flexbox.FlexboxLayout>
</LinearLayout>
```

Need FlexboxLayout dependency in build.gradle:
```
implementation 'com.google.android.flexbox:flexbox:3.0.0'
```

#### view_typing_indicator.xml

```xml
<LinearLayout
    android:layout_width="wrap_content"
    android:layout_height="@dimen/typing_indicator_height"
    android:orientation="horizontal"
    android:gravity="center"
    android:background="@drawable/bg_typing_indicator"
    android:paddingHorizontal="@dimen/space_16"
    android:paddingVertical="@dimen/space_8"
    android:layout_marginStart="@dimen/space_16"
    android:layout_marginTop="@dimen/space_8">

    <View android:id="@+id/dot1"
        android:layout_width="@dimen/typing_dot_size"
        android:layout_height="@dimen/typing_dot_size"
        android:layout_marginEnd="@dimen/typing_dot_gap"
        android:background="@drawable/bg_typing_dot" />
    <View android:id="@+id/dot2"
        android:layout_width="@dimen/typing_dot_size"
        android:layout_height="@dimen/typing_dot_size"
        android:layout_marginEnd="@dimen/typing_dot_gap"
        android:background="@drawable/bg_typing_dot" />
    <View android:id="@+id/dot3"
        android:layout_width="@dimen/typing_dot_size"
        android:layout_height="@dimen/typing_dot_size"
        android:background="@drawable/bg_typing_dot" />
</LinearLayout>
```

Typing animation: in ChatAdapter, when binding the TYPING ViewType, animate dots with staggered ObjectAnimator:

```kotlin
val dots = listOf(holder.dot1, holder.dot2, holder.dot3)
dots.forEachIndexed { index, dot ->
    dot.animate().apply {
        scaleY(0.4f).setDuration(200).setStartDelay(index * 200L)
            .setInterpolator(DecelerateInterpolator())
        withEndAction {
            dot.animate().scaleY(1f).setDuration(200)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    // loop: would need a Runnable; for MVP, just start animation once
                }
        }
    }
}
```

For proper infinite loop, use an AnimationSet or a Handler-based runnable. MVP: fire once on bind.

### Gotchas — Phase 3

- **FlexboxLayout:** Dependency must be added to `app/build.gradle.kts` (or `.gradle`). Version 3.0.0 is stable.
- **EditText maxLines:** `android:maxLines="5"` + `inputType="textMultiLine"` — correct. At 5 lines the field should internally scroll. But the INPUT BAR maxHeight=120dp acts as a container constraint. Set `app:layout_constraintHeight_max="120dp"` on the inputBar.
- **Input bar bottom inset:** Must account for system nav bar. Use `WindowInsetsCompat` in Fragment to add bottom padding: `binding.inputBar.updatePadding(bottom = insets.bottom)`.
- **Toolbar 92% alpha surface:** Can't use a static color. In Fragment's `onViewCreated`: `binding.toolbar.setBackgroundColor(ColorUtils.setAlphaComponent(surfaceColor, (255 * 0.92).toInt()))`.
- **Send button morph animation:** The scale-down→swap→scale-up approach is simple but won't look like a true morph. For a proper morph, use `AnimatedVectorDrawable` or Lottie. MVP uses scale trick; swap to AnimatedVectorDrawable later.
- **Typing indicator infinite loop:** A proper infinite staggered animation requires an AnimatorSet with `setRepeatCount(INFINITE)`. Implement in a `TypingIndicatorView` custom view or use:
  ```kotlin
  val set = AnimatorSet()
  set.playTogether(
      ObjectAnimator.ofFloat(dot1, "scaleY", 1f, 0.4f, 1f).apply { repeatCount = INFINITE },
      ObjectAnimator.ofFloat(dot2, "scaleY", 1f, 0.4f, 1f).apply { startDelay = 200; repeatCount = INFINITE },
      ObjectAnimator.ofFloat(dot3, "scaleY", 1f, 0.4f, 1f).apply { startDelay = 400; repeatCount = INFINITE }
  )
  set.duration = 1200
  set.start()
  ```

### Test Checkpoint — Phase 3

1. Input bar is pill-shaped (28dp radius), surfaceVariant fill, 2dp shadow (light mode only)
2. Attach button visible on left, 40×40dp with add_circle_outline icon
3. EditText fills remaining space, hint "Ask anything...", max 5 lines with internal scroll
4. No text → send button is microphone icon, transparent bg
5. Type text → send button morphs to arrow_upward, primary bg
6. Press send → button becomes stop icon, red bg; text field clears
7. After response completes → button returns to mic icon
8. Empty chat shows welcome icon (80dp, 80% alpha), headline, subtitle, 4 suggestion chips
9. Tapping a suggestion chip fills the input field
10. Typing indicator: 3 dots at 60% alpha, staggered scale animation, left-aligned like assistant bubble
11. System nav bar inset handled — input bar sits above nav bar

---

## Phase 4: Chat List & Navigation Polish

**Goal:** ChatListFragment and ChatListAdapter styled per spec, navigation transitions (fadeThrough), consistent top app bar styling.

**Dependencies:** Phase 1 (theme, colors, typography)

### Files to CREATE

| # | File | Purpose |
|---|------|---------|
| 1 | `app/src/main/res/anim/fade_through.xml` | Navigation fadeThrough anim: alpha 0→1, 300ms FastOutSlowIn |
| 2 | `app/src/main/res/anim/fade_out.xml` | Exit fade: alpha 1→0, 150ms |
| 3 | `app/src/main/res/drawable/bg_chat_list_item.xml` | List item bg: surface fill, 12dp radius, selectableItemBackground ripple |

### Files to MODIFY

| # | File | Change |
|---|------|--------|
| 4 | `app/src/main/res/layout/fragment_chat_list.xml` | Restyle: toolbar specs, RecyclerView padding, FAB styling |
| 5 | `app/src/main/res/layout/item_chat_list.xml` | Restyle: surface card, titleLarge for name, bodySmall for preview, labelSmall for time |
| 6 | `app/src/main/java/com/example/qwen3chat/ui/chatlist/ChatListAdapter.kt` | Apply TextAppearance styles, date formatting, selection highlight |
| 7 | `app/src/main/java/com/example/qwen3chat/ui/chatlist/ChatListFragment.kt` | Toolbar setup, navigate with fadeThrough transition |
| 8 | `app/src/main/res/navigation/nav_graph.xml` | Add enter/exit animation references to action destinations |

### Detailed Changes

#### fragment_chat_list.xml

```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:background="@color/background">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="0dp"
        android:layout_height="@dimen/toolbar_height"
        android:paddingHorizontal="@dimen/screen_h_padding"
        android:background="@color/toolbar_surface"
        app:title="Chats"
        app:titleTextAppearance="@style/TextAppearance.Qwen.TitleLarge"
        .../>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:paddingHorizontal="@dimen/screen_h_padding"
        android:paddingTop="@dimen/space_8"
        android:clipToPadding="false"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toBottomOf="parent"
        .../>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabNewChat"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_margin="@dimen/space_16"
        android:src="@drawable/ic_add"
        app:backgroundTint="@color/primary_container"
        app:tint="@color/on_primary_container"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />
</ConstraintLayout>
```

#### item_chat_list.xml

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginVertical="@dimen/space_2"
    app:cardElevation="0dp"
    app:cardBackgroundColor="@color/surface"
    app:cardCornerRadius="@dimen/space_12"
    app:strokeWidth="0dp">

    <LinearLayout
        android:orientation="horizontal"
        android:padding="@dimen/list_item_h_padding">

        <!-- Avatar placeholder -->
        <ImageView android:id="@+id/ivAvatar"
            android:layout_width="48dp" android:layout_height="48dp"
            android:src="@drawable/ic_model_avatar"
            android:layout_marginEnd="@dimen/space_12" />

        <LinearLayout
            android:orientation="vertical"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_gravity="center_vertical">

            <TextView android:id="@+id/tvChatName"
                android:textAppearance="@style/TextAppearance.Qwen.TitleMedium"
                android:textColor="@color/on_surface"
                android:maxLines="1"
                android:ellipsize="end" />

            <TextView android:id="@+id/tvLastMessage"
                android:textAppearance="@style/TextAppearance.Qwen.BodySmall"
                android:textColor="@color/on_surface_variant"
                android:maxLines="2"
                android:ellipsize="end"
                android:layout_marginTop="@dimen/space_2" />
        </LinearLayout>

        <LinearLayout
            android:orientation="vertical"
            android:gravity="end"
            android:layout_marginStart="@dimen/space_8">

            <TextView android:id="@+id/tvTime"
                android:textAppearance="@style/TextAppearance.Qwen.LabelSmall"
                android:textColor="@color/on_surface_variant" />

            <TextView android:id="@+id/tvModelBadge"
                android:textAppearance="@style/TextAppearance.Qwen.LabelSmall"
                android:textColor="@color/primary"
                android:background="@drawable/bg_model_badge"
                android:paddingHorizontal="@dimen/space_6"
                android:paddingVertical="@dimen/space_2"
                android:layout_marginTop="@dimen/space_4" />
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

#### ChatListAdapter.kt changes

- Format timestamp: "Today" / "Yesterday" / date for display
- `tvChatName.typeface` via ResourcesCompat (or rely on TextAppearance style already applied in XML)
- Model badge: apply `bg_model_badge.xml` with `@color/primary_container` background, `@color/on_primary_container` text

#### Navigation transitions (nav_graph.xml)

In `<action>` elements for chat detail:

```xml
<action
    android:id="@+id/action_chatList_to_chatDetail"
    app:destination="@id/chatDetailFragment"
    app:enterAnim="@anim/fade_through"
    app:exitAnim="@anim/fade_out"
    app:popEnterAnim="@anim/fade_through"
    app:popExitAnim="@anim/fade_out" />
```

Also apply to model selection → main activity transition.

### Gotchas — Phase 4

- **MaterialCardView vs surface background:** Using `MaterialCardView` adds elevation. Set `app:cardElevation="0dp"` for flat cards matching the spec.
- **FAB color:** Spec doesn't explicitly mention FAB. Use `primary_container` bg + `on_primary_container` icon per design system.
- **Navigation animations:** Requires Navigation 2.6+. Check current `androidx.navigation:navigation-fragment-ktx` version.
- **Avatar placeholder:** Use a simple circle drawable with model initial/icon. Not a full image loading system in this phase.

### Test Checkpoint — Phase 4

1. Chat list items: surface background, 12dp rounded corners, 2dp vertical margin
2. Title: titleMedium (16sp, Medium 500), onSurface color
3. Preview: bodySmall (12sp, Regular 400), onSurfaceVariant, max 2 lines
4. Time: labelSmall, onSurfaceVariant, right-aligned
5. Model badge: labelSmall, primary color, primary_container background, pill shape
6. FAB: primary_container bg, on_primary_container icon, 16dp margin
7. Navigate to chat detail → fadeThrough transition (300ms)
8. Toolbar: 56dp height, titleLarge title, surface bg

---

## Phase 5: Download & Model Selection Polish

**Goal:** Download screen matches DESIGN_SPEC §11 (carousel cards, progress bar, speed/ETA, download button states). Model selection screen polished. Theme toggle animation.

**Dependencies:** Phase 1 (theme), Phase 4 (navigation transitions)

### Files to CREATE

| # | File | Purpose |
|---|------|---------|
| 1 | `app/src/main/res/drawable/bg_download_button_idle.xml` | Pill (28dp radius), primary fill |
| 2 | `app/src/main/res/drawable/bg_download_button_cancel.xml` | Pill, surfaceVariant fill |
| 3 | `app/src/main/res/drawable/bg_download_button_complete.xml` | Pill, #4CAF50 fill |
| 4 | `app/src/main/res/drawable/bg_carousel_card.xml` | 20dp radius, surface fill, 1dp outlineVariant stroke |
| 5 | `app/src/main/res/drawable/bg_progress_track.xml` | 3dp radius, surfaceVariant fill |
| 6 | `app/src/main/res/drawable/bg_progress_fill.xml` | 3dp radius, primary fill |
| 7 | `app/src/main/res/anim/theme_toggle_reveal.xml` | CircularReveal animation: clip radius 0→max, 400ms |
| 8 | `app/src/main/res/anim/carousel_scale.xml` | Carousel card scale: 0.92→1.0 (selected), 1.0→0.92 (unselected) |
| 9 | `app/src/main/res/drawable/bg_radio_option.xml` | Radio option bg: surfaceVariant fill, 12dp radius |

### Files to MODIFY

| # | File | Change |
|---|------|--------|
| 10 | `app/src/main/res/layout/activity_main.xml` | Restyle: header with headlineMedium, carousel section, progress section with speed/ETA/percentage, download button |
| 11 | `app/src/main/res/layout/item_carousel_card.xml` | Restyle: 200dp height, 20dp corners, icon 48×48, titleLarge, bodySmall desc, labelMedium size |
| 12 | `app/src/main/java/com/example/qwen3chat/CarouselAdapter.kt` | Update card binding: scale animation for selected/unselected, alpha change (1.0/0.6), apply new colors |
| 13 | `app/src/main/java/com/example/qwen3chat/MainActivity.kt` | Download button state machine (IDLE/DOWNLOADING/COMPLETE), progress bar binding, speed/ETA formatting, indeterminate sweep |
| 14 | `app/src/main/java/com/example/qwen3chat/ModelDownloadService.kt` | Add progress/speed/ETA callbacks for UI binding |
| 15 | `app/src/main/res/layout/activity_model_selection.xml` | Restyle: radio options with new colors, titleLarge header |
| 16 | `app/src/main/java/com/example/qwen3chat/ModelSelectionActivity.kt` | Apply TextAppearance, new colors, fadeThrough transition |

### Detailed Changes

#### activity_main.xml (download screen restructure)

```xml
<ScrollView android:background="@color/background">

    <LinearLayout android:orientation="vertical"
        android:padding="@dimen/screen_h_padding">

        <!-- Header -->
        <TextView
            android:text="Download Model"
            android:textAppearance="@style/TextAppearance.Qwen.HeadlineMedium"
            android:textColor="@color/on_surface"
            android:layout_marginTop="@dimen/space_48"
            android:layout_marginBottom="@dimen/space_8" />

        <TextView
            android:text="Choose an on-device model to get started"
            android:textAppearance="@style/TextAppearance.Qwen.BodyMedium"
            android:textColor="@color/on_surface_variant"
            android:layout_marginBottom="@dimen/space_32" />

        <!-- Carousel -->
        <androidx.viewpager2.widget.ViewPager2
            android:id="@+id/viewPager"
            android:layout_width="match_parent"
            android:layout_height="240dp"
            android:clipToPadding="false"
            android:paddingHorizontal="@dimen/space_16" />

        <!-- Progress section (visible during download) -->
        <LinearLayout android:id="@+id/progressSection"
            android:visibility="gone"
            android:orientation="vertical"
            android:layout_marginTop="@dimen/space_24">

            <!-- Progress bar container -->
            <FrameLayout
                android:layout_width="match_parent"
                android:layout_height="@dimen/download_progress_height">

                <!-- Track -->
                <View android:background="@drawable/bg_progress_track"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent" />

                <!-- Fill (animated width) -->
                <View android:id="@+id/progressFill"
                    android:background="@drawable/bg_progress_fill"
                    android:layout_width="0dp"
                    android:layout_height="match_parent" />
            </FrameLayout>

            <!-- Speed / ETA row -->
            <LinearLayout
                android:orientation="horizontal"
                android:layout_marginTop="@dimen/space_8">

                <TextView android:id="@+id/tvSpeed"
                    android:text="12.3 MB/s"
                    android:textAppearance="@style/TextAppearance.Qwen.LabelMedium"
                    android:textColor="@color/on_surface_variant" />

                <View android:layout_width="0dp" android:layout_weight="1" />

                <TextView android:id="@+id/tvEta"
                    android:text="2 min remaining"
                    android:textAppearance="@style/TextAppearance.Qwen.LabelMedium"
                    android:textColor="@color/on_surface_variant" />
            </LinearLayout>

            <!-- Percentage -->
            <TextView android:id="@+id/tvPercentage"
                android:text="42%"
                android:textAppearance="@style/TextAppearance.Qwen.HeadlineSmall"
                android:textColor="@color/primary"
                android:gravity="center"
                android:layout_marginTop="@dimen/space_16" />
        </LinearLayout>

        <!-- Download button -->
        <Button android:id="@+id/btnDownload"
            android:layout_width="match_parent"
            android:layout_height="@dimen/download_button_height"
            android:text="Download"
            android:textAppearance="@style/TextAppearance.Qwen.LabelLarge"
            android:textColor="@color/on_primary"
            android:background="@drawable/bg_download_button_idle"
            android:layout_marginTop="@dimen/space_32"
            android:stateListAnimator="@null" />
    </LinearLayout>
</ScrollView>
```

#### item_carousel_card.xml (restyle)

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="@dimen/download_card_height"
    app:cardCornerRadius="@dimen/download_card_radius"
    app:cardBackgroundColor="@color/surface"
    app:strokeWidth="1dp"
    app:strokeColor="@color/outline_variant"
    app:cardElevation="0dp"
    android:layout_marginHorizontal="@dimen/space_4">

    <LinearLayout
        android:orientation="vertical"
        android:gravity="center"
        android:padding="@dimen/download_card_padding">

        <ImageView android:id="@+id/ivModelIcon"
            android:layout_width="@dimen/download_icon_size"
            android:layout_height="@dimen/download_icon_size"
            android:src="@drawable/ic_model_qwen" />

        <TextView android:id="@+id/tvModelName"
            android:textAppearance="@style/TextAppearance.Qwen.TitleLarge"
            android:textColor="@color/on_surface"
            android:layout_marginTop="@dimen/space_12" />

        <TextView android:id="@+id/tvModelDesc"
            android:textAppearance="@style/TextAppearance.Qwen.BodySmall"
            android:textColor="@color/on_surface_variant"
            android:gravity="center"
            android:maxLines="2"
            android:ellipsize="end"
            android:layout_marginTop="@dimen/space_4" />

        <TextView android:id="@+id/tvModelSize"
            android:textAppearance="@style/TextAppearance.Qwen.LabelMedium"
            android:textColor="@color/on_surface_variant"
            android:layout_marginTop="@dimen/space_8" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

#### CarouselAdapter.kt changes

Use `ViewPager2.PageTransformer` for scale+alpha effect:

```kotlin
class CarouselScaleTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        val absPos = abs(position)
        page.apply {
            scaleX = 1f - (0.08f * absPos)
            scaleY = 1f - (0.08f * absPos)
            alpha = 1f - (0.4f * absPos)
        }
    }
}
```

In `MainActivity.onCreate()`:
```kotlin
binding.viewPager.setPageTransformer(CarouselScaleTransformer())
binding.viewPager.offscreenPageLimit = 1
```

#### Download button state machine (MainActivity)

```kotlin
enum class DownloadState { IDLE, DOWNLOADING, COMPLETE }

private var downloadState = DownloadState.IDLE

private fun updateDownloadButton(state: DownloadState) {
    downloadState = state
    val (text, bg, textColor) = when (state) {
        DownloadState.IDLE -> Triple("Download", R.drawable.bg_download_button_idle, R.color.on_primary)
        DownloadState.DOWNLOADING -> Triple("Cancel", R.drawable.bg_download_button_cancel, R.color.on_surface)
        DownloadState.COMPLETE -> Triple("Open", R.drawable.bg_download_button_complete, R.color.on_primary)
    }
    binding.btnDownload.apply {
        setText(text)
        setBackgroundResource(bg)
        setTextColor(ContextCompat.getColor(this@MainActivity, textColor))
        // Morph animation
        animate().scaleX(0.95f).scaleY(0.95f).setDuration(125).withEndAction {
            animate().scaleX(1f).scaleY(1f).setDuration(125).start()
        }.start()
    }
}
```

#### Progress bar binding

When `ModelDownloadService` broadcasts progress:
```kotlin
// In MainActivity broadcast receiver
val progress = intent.getFloatExtra("progress", 0f) // 0.0 to 1.0
val speedBytes = intent.getLongExtra("speed", 0L)
val etaSeconds = intent.getLongExtra("eta", 0L)

// Update progress fill width
val containerWidth = binding.progressFill.parent?.let {
    (it as View).width
} ?: 0
binding.progressFill.layoutParams.width = (containerWidth * progress).toInt()
binding.progressFill.requestLayout()

// Update text
binding.tvPercentage.text = "${(progress * 100).toInt()}%"
binding.tvSpeed.text = formatSpeed(speedBytes)
binding.tvEta.text = formatEta(etaSeconds)
```

#### Progress indeterminate mode

For indeterminate (when speed is unknown), use a gradient sweep instead of solid fill:
```xml
<!-- bg_progress_indeterminate.xml -->
<layer-list>
    <item android:id="@android:id/background">
        <shape android:shape="rectangle">
            <corners android:radius="@dimen/download_progress_radius"/>
            <solid android:color="@color/surface_variant"/>
        </shape>
    </item>
    <item android:id="@android:id/progress">
        <clip>
            <shape>
                <corners android:radius="@dimen/download_progress_radius"/>
                <gradient
                    android:startColor="@color/primary"
                    android:centerColor="@color/secondary"
                    android:endColor="@color/primary"
                    android:angle="0"/>
            </shape>
        </clip>
    </item>
</layer-list>
```

Animate the gradient sweep using a ValueAnimator on the progress (0→10000, looped).

#### Theme toggle animation (circularReveal)

If theme toggle exists in settings/toolbar:
```kotlin
// Get the toggle button's screen coordinates
val cx = toggleButton.x + toggleButton.width / 2
val cy = toggleButton.y + toggleButton.height / 2

// Animate reveal
val finalRadius = max(rootView.width, rootView.height).toFloat()
val anim = ViewAnimationUtils.createCircularReveal(rootView, cx.toInt(), cy.toInt(), 0f, finalRadius)
anim.duration = 400
anim.interpolator = FastOutSlowInInterpolator()
anim.start()
```

### Gotchas — Phase 5

- **ViewPager2 PageTransformer:** Runs on every scroll frame. Keep `abs(position)` math simple. `offscreenPageLimit=1` ensures at least 3 pages (center + 2 neighbors) for smooth scale transitions.
- **Progress bar indeterminate:** A gradient sweep animation is GPU-intensive on low-end devices. Alternative: use Material `IndeterminateDrawable` with custom colors via `CircularProgressIndicator` or `LinearProgressIndicator` from Material3.
- **Download button morph:** The scale-down→scale-up trick works for basic feedback. For production, consider `AnimatedVectorDrawable` between icon states.
- **ModelSelection radio buttons:** Replace stock RadioButton with Material-style chips or cards with selection highlight. Use `MaterialCardView` with `app:checkedIcon` pattern.
- **Progress fill width animation:** `layoutParams.width` change without animation is jarring. Use `ObjectAnimator.ofInt(target, "width", oldWidth, newWidth)` or ValueAnimator.

### Test Checkpoint — Phase 5

1. Download screen: headline "Download Model" + subtitle, styled per spec
2. Carousel: cards are 200dp tall, 20dp radius, surface bg with 1dp outlineVariant border
3. Carousel scroll: center card scale=1.0 alpha=1.0; neighboring cards scale=0.92 alpha=0.6
4. Card content: 48dp icon, titleLarge name, bodySmall description, labelMedium size
5. During download: progress section visible, 6dp bar with primary fill, speed left, ETA right, percentage centered
6. Download button: fills screen width, 56dp height, pill shape
   - Idle: "Download" on primary bg
   - Downloading: "Cancel" on surfaceVariant bg
   - Complete: "Open" on green (#4CAF50) bg
7. Model selection: titleLarge header, options with surfaceVariant bg and 12dp radius
8. Theme toggle: circularReveal from toggle button, 400ms

---

## Implementation Order Recap

```
Phase 1: Theme Foundation      (blocking — must be first)
    ↓
Phase 2: Message Bubbles       (depends on Phase 1)
    ↓
Phase 3: Input Bar & Empty     (depends on Phases 1 + 2)
    ↓
Phase 4: Chat List & Nav       (depends on Phase 1, independent of 2-3)
    ↓
Phase 5: Download Polish       (depends on Phase 1, independent of 2-4)
```

Phases 4 and 5 can be parallelized if desired (they only depend on Phase 1).

---

## Global Gotchas

1. **Inter font:** All phases reference `@style/TextAppearance.Qwen.*` which expect Inter font in `res/font/`. If font files aren't added, everything falls back to sans-serif. Add font files as a setup step before Phase 2.
2. **JetBrains Mono:** Required for code blocks. Same deal — add `.ttf` to `res/font/`.
3. **Night mode testing:** Use `adb shell cmd uimode night yes/no` to toggle without system settings.
4. **ViewBinding:** All fragments/activities must use ViewBinding (not findViewById). Verify `buildFeatures { viewBinding = true }` in `app/build.gradle.kts`.
5. **ConstraintLayout version:** The `matchConstraintMaxWidth` feature requires ConstraintLayout 2.0+. Verify the dependency.
6. **FlexboxLayout:** Phase 3 adds this dependency. If build fails, it's missing from `build.gradle`.
7. **Material3 version:** Toolbar, CardView, FAB, and color attributes must match Material3 1.1+. Verify `com.google.android.material:material` version.
8. **Legacy drawable references:** Files being deleted (`bg_message_user.xml`, `bg_message_bot.xml`) must be unreferenced everywhere before deletion. Search with `grep -r "bg_message_user\|bg_message_bot"` first.
