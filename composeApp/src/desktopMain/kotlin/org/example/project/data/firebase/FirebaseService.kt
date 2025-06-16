package org.example.project.data.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.models.AttendanceRecord
import java.io.FileInputStream
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FirebaseService {
    private var firestore: Firestore? = null

    suspend fun initialize(): Boolean {
        return try {
            val serviceAccountPath = "/Users/ansh/Downloads/SmartAttendAdmin/composeApp/src/desktopMain/composeResources/files/service-account-key.json"
            val serviceAccount = FileInputStream(serviceAccountPath)
            val credentials = GoogleCredentials.fromStream(serviceAccount)

            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }

            firestore = FirestoreClient.getFirestore()
            println("Smart Attend: Firebase initialized successfully")
            true
        } catch (e: Exception) {
            println("Smart Attend: Firebase initialization failed - ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun convertTimestamp(timestampObj: Any?): String {
        return try {
            when (timestampObj) {
                is Timestamp -> {
                    val date = Date((timestampObj.seconds * 1000 + timestampObj.nanos / 1000000).toLong())
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
                }
                is String -> timestampObj
                else -> timestampObj?.toString() ?: ""
            }
        } catch (e: Exception) {
            timestampObj?.toString() ?: ""
        }
    }

    suspend fun getAttendanceRecords(
        year: Int,
        month: Int,
        group: String? = null,
        subject: String? = null
    ): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        return@withContext try {
            val collectionName = "attendance_${year}_${month.toString().padStart(2, '0')}"
            var query: Query = firestore!!.collection(collectionName)

            group?.let { query = query.whereEqualTo("group", it) }
            subject?.let { query = query.whereEqualTo("subject", it) }

            val snapshot = query.get().get()

            snapshot.documents.mapNotNull { doc ->
                try {
                    AttendanceRecord(
                        date = doc.getString("date") ?: "",
                        deviceRoom = doc.getString("deviceRoom") ?: "",
                        group = doc.getString("group") ?: "",
                        isExtra = doc.getBoolean("isExtra") ?: false,
                        present = doc.getBoolean("present") ?: false,
                        rollNumber = doc.getString("rollNumber") ?: "",
                        subject = doc.getString("subject") ?: "",
                        timestamp = convertTimestamp(doc.get("timestamp")),
                        type = doc.getString("type") ?: ""
                    )
                } catch (e: Exception) {
                    println("Error mapping record: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching attendance records: ${e.message}")
            emptyList()
        }
    }

    suspend fun getStudentAttendance(
        rollNumber: String,
        year: Int,
        month: Int
    ): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        return@withContext try {
            val collectionName = "attendance_${year}_${month.toString().padStart(2, '0')}"
            val snapshot = firestore!!.collection(collectionName)
                .whereEqualTo("rollNumber", rollNumber)
                .get()
                .get()

            snapshot.documents.mapNotNull { doc ->
                try {
                    AttendanceRecord(
                        date = doc.getString("date") ?: "",
                        deviceRoom = doc.getString("deviceRoom") ?: "",
                        group = doc.getString("group") ?: "",
                        isExtra = doc.getBoolean("isExtra") ?: false,
                        present = doc.getBoolean("present") ?: false,
                        rollNumber = doc.getString("rollNumber") ?: "",
                        subject = doc.getString("subject") ?: "",
                        timestamp = convertTimestamp(doc.get("timestamp")),
                        type = doc.getString("type") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching student attendance: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllSubjects(year: Int, month: Int): List<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val collectionName = "attendance_${year}_${month.toString().padStart(2, '0')}"
            val snapshot = firestore!!.collection(collectionName).get().get()

            snapshot.documents
                .mapNotNull { it.getString("subject") }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            println("Error fetching subjects: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllGroups(year: Int, month: Int): List<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val collectionName = "attendance_${year}_${month.toString().padStart(2, '0')}"
            val snapshot = firestore!!.collection(collectionName).get().get()

            snapshot.documents
                .mapNotNull { it.getString("group") }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            println("Error fetching groups: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAvailableMonths(year: Int): List<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val collections = firestore!!.listCollections()
            val attendanceCollections = collections
                .map { it.id }
                .filter { it.startsWith("attendance_${year}_") }
                .mapNotNull {
                    val monthStr = it.substringAfterLast("_")
                    monthStr.toIntOrNull()
                }
                .sorted()

            if (attendanceCollections.isEmpty()) {
                // Return current month if no collections found
                listOf(Calendar.getInstance().get(Calendar.MONTH) + 1)
            } else {
                attendanceCollections
            }
        } catch (e: Exception) {
            println("Error fetching available months: ${e.message}")
            listOf(Calendar.getInstance().get(Calendar.MONTH) + 1)
        }
    }
}