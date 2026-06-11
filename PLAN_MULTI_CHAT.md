# Multi-Chat Architecture Plan (by Pat)

## Data Model
- ChatEntity: id (PK), title, modelUsed, createdAt, updatedAt
- MessageEntity: id (PK), chatId (FK → Chat, CASCADE), role, content, timestamp

## Persistence: Room
- Compile-time SQL verification, @Relation, Flow/LiveData, Migration support
- NOT SharedPreferences+JSON (no FK, no queries, loads all into memory)
- NOT raw SQLite (manual cursor mapping, no compile-time checks)

## Navigation: Single-Activity + Fragments + NavGraph
- Shared ViewModel scoping, type-safe args, auto back-stack, transitions
- NOT multiple Activities (heavyweight, slow transitions, no shared ViewModel)

## Auto-Title Logic
```
fun generateTitle(firstMessage: String): String {
    val cleaned = firstMessage.trim()
    if (cleaned.isEmpty()) return "New Chat"
    if (cleaned.length <= 50) return cleaned
    val truncated = cleaned.take(50)
    val lastSpace = truncated.lastIndexOf(' ')
    return if (lastSpace > 20) truncated.take(lastSpace) + "…" else truncated + "…"
}
```
Save message FIRST, then update title, both in @Transaction.

## New Files
- data/entity/ChatEntity.kt, MessageEntity.kt
- data/dao/ChatDao.kt, MessageDao.kt
- data/db/AppDatabase.kt
- data/repository/ChatRepository.kt
- ui/chatlist/ChatListFragment.kt, ChatListAdapter.kt, ChatListViewModel.kt
- ui/chatdetail/ChatDetailFragment.kt, ChatDetailViewModel.kt
- res/navigation/nav_graph.xml

## Implementation Order
1. Data foundation (Room entities, DAOs, database, repository)
2. Chat list screen (ViewModel, Adapter, Fragment, FAB)
3. Chat detail refactor (load from Room, save to Room, auto-title)
4. Navigation wiring (nav_graph.xml, MainActivity as NavHost)
5. Polish (swipe-delete, rename, dark theme, empty state)

## Gotchas
1. Room Flow re-emits on ANY table write — use distinctUntilChanged()
2. Auto-title race — save message before title, in @Transaction
3. Delete while detail open — handle null/orphaned chat gracefully
4. Destructive migration wipes data — write proper Migration objects
5. RecyclerView adapter Context leaks — use itemView.context, not field
