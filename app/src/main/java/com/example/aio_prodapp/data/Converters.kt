package com.example.aio_prodapp.data

import androidx.room.TypeConverter

/** Room has no CHECK annotation, so enum columns go through these converters (spec 3.2). */
class Converters {
    @TypeConverter fun fromPriority(value: TaskPriority): String = value.name
    @TypeConverter fun toPriority(value: String): TaskPriority = TaskPriority.fromName(value)

    @TypeConverter fun fromActivityType(value: ActivityType): String = value.name
    @TypeConverter fun toActivityType(value: String): ActivityType = ActivityType.valueOf(value)

    @TypeConverter fun fromNotificationType(value: NotificationType): String = value.name
    @TypeConverter fun toNotificationType(value: String): NotificationType = NotificationType.valueOf(value)
}
