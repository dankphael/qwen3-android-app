# Qwen3 Chat App — UI/UX Design Spec (by Pat)

Reference document for Suresh to execute as code. All values are exact.

---

## 1. DARK THEME COLOR PALETTE

| Token | Hex |
|-------|-----|
| background | #0D0D0D |
| surface | #1A1A1A |
| surfaceVariant | #262626 |
| primary | #7C6FF7 |
| primaryContainer | #2D2566 |
| onPrimary | #FFFFFF |
| onPrimaryContainer | #E0DBFF |
| secondary | #FFB74D |
| secondaryContainer | #3E2D14 |
| onSecondary | #1A0D00 |
| onSecondaryContainer | #FFE0B2 |
| error | #FF5252 |
| onError | #FFFFFF |
| onSurface | #EDEDED |
| onSurfaceVariant | #B0B0B0 |
| outline | #404040 |
| outlineVariant | #2E2E2E |
| inverseSurface | #F0F0F0 |
| inverseOnSurface | #1A1A1A |

## 2. LIGHT THEME COLOR PALETTE

| Token | Hex |
|-------|-----|
| background | #FAFAFA |
| surface | #FFFFFF |
| surfaceVariant | #F0EEFC |
| primary | #6C5CE7 |
| primaryContainer | #E8E5FF |
| onPrimary | #FFFFFF |
| onPrimaryContainer | #1E1666 |
| secondary | #F09B2C |
| secondaryContainer | #FFF3E0 |
| onSecondary | #FFFFFF |
| onSecondaryContainer | #4E2D00 |
| error | #E53935 |
| onError | #FFFFFF |
| onSurface | #1A1A1A |
| onSurfaceVariant | #5C5C5C |
| outline | #CBCBCB |
| outlineVariant | #E8E8E8 |
| inverseSurface | #2A2A2A |
| inverseOnSurface | #F5F5F5 |

## 3. TYPOGRAPHY

Font family: Inter (system fallback: sans-serif).
Monospace: JetBrains Mono (fallback: monospace).
Format: fontSize(sp) | fontWeight | lineHeight(sp) | letterSpacing(sp)

| Style | Size | Weight | LineH | Letter |
|-------|------|--------|-------|--------|
| displayLarge | 57sp | Bold(700) | 64sp | -0.25sp |
| displayMedium | 45sp | Bold(700) | 52sp | 0.00sp |
| displaySmall | 36sp | Bold(700) | 44sp | 0.00sp |
| headlineLarge | 32sp | SemiBold(600) | 40sp | 0.00sp |
| headlineMedium | 28sp | SemiBold(600) | 36sp | 0.00sp |
| headlineSmall | 24sp | SemiBold(600) | 32sp | 0.00sp |
| titleLarge | 22sp | Medium(500) | 28sp | 0.00sp |
| titleMedium | 16sp | Medium(500) | 24sp | 0.15sp |
| titleSmall | 14sp | Medium(500) | 20sp | 0.10sp |
| bodyLarge | 16sp | Regular(400) | 24sp | 0.50sp |
| bodyMedium | 14sp | Regular(400) | 20sp | 0.25sp |
| bodySmall | 12sp | Regular(400) | 16sp | 0.40sp |
| labelLarge | 14sp | Medium(500) | 20sp | 0.10sp |
| labelMedium | 12sp | Medium(500) | 16sp | 0.50sp |
| labelSmall | 11sp | Medium(500) | 16sp | 0.50sp |
| code | 14sp | Regular(400) | 20sp | 0.00sp |

## 4. SPACING

- Base unit: 4dp
- Scale: 0, 2, 4, 6, 8, 12, 16, 20, 24, 32, 40, 48, 56, 64 dp
- Screen horizontal padding: 16dp
- Card internal padding: 16dp
- List item: 12dp H, 8dp V
- Section gap: 24dp

## 5. CHAT SCREEN LAYOUT

### Toolbar (TopAppBar)
- Height: 56dp
- Horizontal padding: 16dp
- Left icon (menu): 24×24dp, 12dp marginEnd
- Title: titleLarge (22sp), Medium(500), centeredVertically
- Right icon area: 3 icons × 40dp tap target, 4dp gap
- Bottom divider: none
- Surface color with 92% alpha

### Message List
- Top padding: 8dp
- Bottom padding: 16dp
- Item spacing: 4dp between consecutive messages
- Section spacing: 16dp (date groups)

### Input Bar
- Bottom-anchored above nav bar inset
- Elevation: 0dp
- Top divider: 0.5dp, outlineVariant

## 6. MESSAGE BUBBLES

### User Bubble
- bgColor: primary
- textColor: onPrimary (#FFFFFF)
- cornerRadius: 20dp (all corners)
- padding: H16 V12
- margin: start 64dp, end 16dp, top 2dp, bottom 2dp
- alignment: end (right-aligned)
- maxWidth: 80% of screen width
- No tail (modern rounded-rect)
- Timestamp: below bubble, alignEnd, 4dp topMargin, labelSmall, onSurfaceVariant, format "HH:mm"

### Assistant Bubble
- bgColor: surfaceVariant
- textColor: onSurface
- cornerRadius: 20dp (all corners)
- padding: H16 V12
- margin: start 16dp, end 64dp, top 2dp, bottom 2dp
- alignment: start (left-aligned)
- maxWidth: 88% of screen width
- Timestamp: below bubble, alignStart

### Code Block (within bubble)
- bgColor: #0D0D0D (dark) / #F5F5F5 (light)
- cornerRadius: 12dp
- padding: 12dp
- font: JetBrains Mono 13sp
- Language label: topRight, labelSmall, onSurfaceVariant
- Copy button: 28×28dp icon, topRight 8dp inset

## 7. THINKING SECTION (Chain-of-Thought)

### Collapsed
- Height: 36dp
- bgColor: transparent
- Content: "Thinking" label + animated dots
- Label: labelMedium (12sp), color=secondary

### Expanded
- bgColor: primaryContainer (40% alpha)
- cornerRadius: 12dp
- padding: H12 V10
- margin: bottom 12dp (gap before answer)
- maxHeight: 300dp (scrollable internally)
- Header: 🧠 icon (18×18dp) + "Thinking" labelMedium + chevron 16×16dp
- Content text: bodySmall (12sp), onPrimaryContainer at 80% opacity

### Animation
- Expand: 300ms, FastOutSlowInEasing
- Collapse: 250ms, FastOutSlowInEasing
- Height: ValueAnimator on height property

## 8. INPUT BAR

### Container
- minHeight: 56dp, maxHeight: 120dp (5 lines then scroll)
- cornerRadius: 28dp (pill)
- bgColor: surfaceVariant
- padding: 4dp
- margin: H12, V8
- Elevation: 2dp (light mode only)

### Attach Button
- Size: 40×40dp (icon 24dp)
- Margin: 4dp from bar start
- Icon: add_circle_outline
- Color: onSurfaceVariant

### Text Field
- Flex: 1 (fills remaining)
- minHeight: 40dp
- Padding: H8 V8
- Text: bodyLarge (16sp)
- Hint: "Ask anything..."
- Max lines: 5

### Send Button
- Size: 40×40dp (icon 24dp), circle
- Margin: 4dp from bar end
- IDLE (no text): mic icon, transparent bg, onSurfaceVariant
- READY (text entered): arrow_upward icon, primary bg, onPrimary
- SENDING: stop icon, error bg (#FF5252), onError
- Morph animation: 200ms, FastOutSlowInEasing

## 9. EMPTY STATE

- Layout: Column, center H+V
- Padding: 32dp horizontal
- Animation: fadeIn 400ms + slideUp 300ms
- Welcome icon: launcher icon 80×80dp, 80% opacity
- Welcome text: "What can I help with?" headlineMedium, center
- Subtitle: "Ask anything — brainstorm, write, code, or just chat" bodyMedium, onSurfaceVariant
- Suggested chips: FlowRow, pill shape (20dp radius), H16 V10 padding
  - 🌤️ "Summarize this article"
  - 🎨 "Create a workout plan"
  - 💻 "Write an email reply"
  - 🍳 "Plan a dinner menu"

## 10. TYPING INDICATOR

- Container: 36dp height, pill (18dp radius), surfaceVariant, H16 V8 padding, margin start 16dp
- 3 dots: 8×8dp, onSurfaceVariant at 60% alpha
- Spacing: 6dp between dots
- Animation: scaleY 1.0→0.4→1.0 per dot, 200ms stagger
- Total cycle: 1200ms, infinite repeat
- Easing: DecelerateInterpolator (squash), AccelerateInterpolator (stretch)

## 11. DOWNLOAD SCREEN

### Header
- Title: "Download Model" headlineMedium
- Subtitle: "Choose an on-device model to get started" bodyMedium, onSurfaceVariant
- Margin: top 48dp, bottom 32dp

### Carousel Cards
- Height: 200dp, cornerRadius 20dp
- Padding: 20dp, bgColor surface, 1dp border outlineVariant
- Scale: selected=1.0, unselected=0.92
- Alpha: selected=1.0, unselected=0.6
- Animation: 300ms FastOutSlowInEasing
- Card content: 48×48dp icon, titleLarge name, bodySmall desc, labelMedium size

### Progress Bar
- Height: 6dp, cornerRadius 3dp
- Track: surfaceVariant, Fill: primary
- Indeterminate: 1500ms gradient sweep cycle

### Speed/ETA
- "12.3 MB/s" left, "2 min remaining" right
- labelMedium, onSurfaceVariant

### Percentage
- "42%" headlineSmall Bold, primary, center

### Download Button
- fillMaxWidth, 56dp height, pill (28dp radius)
- Idle: "Download" primary bg
- Downloading: "Cancel" surfaceVariant bg
- Complete: "Open" #4CAF50 bg
- Animation: 250ms FastOutSlowInEasing

## 12. ANIMATIONS

| Animation | Duration | Interpolator | Details |
|-----------|----------|--------------|---------|
| Message appear | 250ms | FastOutSlowInEasing | fadeIn + slideUp 24dp→0 |
| Thinking expand | 300ms | FastOutSlowInEasing | height expand + content fadeIn + chevron rotate 0→180° |
| Thinking collapse | 250ms | FastOutSlowInEasing | height collapse + content fadeOut + chevron 180→0° |
| Send button morph | 200ms | FastOutSlowInEasing | icon fade+scale swap + bgColor ARgbEvaluator |
| Download progress fill | real-time | LinearInterpolator | ObjectAnimator on progress |
| Download indeterminate | 1500ms/cycle | LinearInterpolator | gradient sweep |
| Screen transition | 300ms | FastOutSlowInEasing | fadeThrough |
| Theme toggle | 400ms | FastOutSlowInEasing | circularReveal from toggle button |

---

## Quick Reference

| Component | Value |
|-----------|-------|
| Toolbar height | 56dp |
| Bubble maxWidth | 80% (user), 88% (assistant) |
| Bubble cornerRadius | 20dp |
| Bubble padding | H16 V12 |
| Input bar minHeight | 56dp |
| Input bar cornerRadius | 28dp (pill) |
| Send button | 40×40dp circle |
| Thinking collapsed | 36dp |
| Thinking expand | 300ms |
| Message appear | 250ms |
| Send morph | 200ms |
| Typing dot | 8×8dp, 6dp gap, 1200ms cycle |
| Download card | 200dp height, scale 0.92 |
| Progress bar | 6dp height, 3dp radius |
| Base unit | 4dp |
