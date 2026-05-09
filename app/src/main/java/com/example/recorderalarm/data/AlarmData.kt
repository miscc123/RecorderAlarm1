package com.example.recorderalarm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Entity ───────────────────────────────────────────────────────────────────

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String = "",
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val repeatDays: String = "",   // "1,2,3,4,5" = Mon–Fri; "" = once
    val recordingPath: String = "", // empty = use default ringtone
    val recordingName: String = ""
)

// ─── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Int): Alarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: Alarm): Long

    @Update
    suspend fun update(alarm: Alarm)

    @Delete
    suspend fun delete(alarm: Alarm)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Int)
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(entities = [Alarm::class], version = 1, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile private var INSTANCE: AlarmDatabase? = null

        fun getInstance(context: android.content.Context): AlarmDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarm_db"
                ).build().also { INSTANCE = it }
            }
    }
}

// ─── Repository ───────────────────────────────────────────────────────────────

class AlarmRepository(private val dao: AlarmDao) {
    val allAlarms: Flow<List<Alarm>> = dao.getAllAlarms()

    suspend fun getById(id: Int) = dao.getAlarmById(id)
    suspend fun insert(alarm: Alarm) = dao.insert(alarm)
    suspend fun update(alarm: Alarm) = dao.update(alarm)
    suspend fun delete(alarm: Alarm) = dao.delete(alarm)
    suspend fun deleteById(id: Int) = dao.deleteById(id)
}
