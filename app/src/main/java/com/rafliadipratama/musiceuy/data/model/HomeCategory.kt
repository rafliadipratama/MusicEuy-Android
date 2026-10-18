package com.rafliadipratama.musiceuy.data.model

enum class HomeCategoryKind {
    BECAUSE_YOU_LIKE,
    MORE_LIKE_ARTIST,
    BECAUSE_YOU_LISTEN,
    TRY_SOMETHING_NEW
}

data class HomeCategoryItem(
    val id: String,
    val kind: HomeCategoryKind,
    val label: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val tracks: List<Track> = emptyList()
)
