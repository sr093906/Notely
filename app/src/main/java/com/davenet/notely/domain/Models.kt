package com.davenet.notely.domain

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.davenet.notely.database.DatabaseNote
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class NoteEntry(
    @PrimaryKey
    var id: Int?,
    var title: String,
    var text: String,
    var date: Long?,
    var reminder: Long?,
    var started: Boolean,
    var color: Int
) : Parcelable {
    fun copy(): DatabaseNote {
        return DatabaseNote(id, title, text, date, reminder, started, color)
    }
}

fun List<NoteEntry>.asDataBaseModel(): List<DatabaseNote> {
    return map {
        DatabaseNote(it.id, it.title, it.text, it.date, it.reminder, it.started, it.color)
    }
}