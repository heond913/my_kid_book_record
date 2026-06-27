package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.BuildConfig
import com.example.data.model.*

@Database(
    entities = [
        Book::class,
        ReadingSession::class,
        BookPhoto::class,
        StatusHistory::class,
        ReadingGoal::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun sessionDao(): SessionDao
    abstract fun photoDao(): PhotoDao
    abstract fun historyDao(): HistoryDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kids_book_journal_db"
                )
                
                // Only allow destructive migration in debug builds to protect user data in production
                if (BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }
                
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
