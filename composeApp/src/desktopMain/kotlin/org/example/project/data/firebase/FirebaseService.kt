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
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll


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
            false
        }
    }

    // FAST: Load all dashboard data in parallel
    suspend fun getDashboardDataFast(
        year: Int,
        month: Int,
        group: String? = null
    ): DashboardData = withContext(Dispatchers.IO) {
        // Run all queries in parallel using async
        val recordsDeferred = async { getAttendanceRecords(year, month, group) }
        val subjectsDeferred = async { getAllSubjects(year, month) }
        val groupsDeferred = async { getAllGroups(year, month) }
        val subjectTotalsDeferred = async { getAllSubjectTotals(subjectsDeferred.await()) }

        // Wait for all to complete
        val records = recordsDeferred.await()
        val subjects = subjectsDeferred.await()
        val groups = groupsDeferred.await()
        val subjectTotals = subjectTotalsDeferred.await()

        DashboardData(records, subjects, groups, subjectTotals)
    }

    // Get total classes for each subject from subjects collection
    suspend fun getAllSubjectTotals(subjects: List<String>): Map<String, SubjectTotals> = withContext(Dispatchers.IO) {
        return@withContext try {
            val subjectTotalsMap = mutableMapOf<String, SubjectTotals>()

            // Get all subject totals in parallel
            val subjectDeferred = subjects.map { subject ->
                async {
                    val doc = firestore!!.collection("subjects").document(subject).get().get()
                    if (doc.exists()) {
                        val data = doc.data ?: return@async null
                        val lectTotal = (data["lect"] as? Number)?.toInt() ?: 0
                        val labTotal = (data["lab"] as? Number)?.toInt() ?: 0
                        val tutTotal = (data["tut"] as? Number)?.toInt() ?: 0

                        subject to SubjectTotals(lectTotal, labTotal, tutTotal)
                    } else null
                }
            }

            subjectDeferred.awaitAll().filterNotNull().toMap()
        } catch (e: Exception) {
            println("Error fetching subject totals: ${e.message}")
            emptyMap()
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
                    null
                }
            }
        } catch (e: Exception) {
            println("Error fetching attendance records: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllSubjects(year: Int, month: Int): List<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val collectionName = "attendance_${year}_${month.toString().padStart(2, '0')}"
            val snapshot = firestore!!.collection(collectionName)
                .select("subject")
                .limit(1000)
                .get()
                .get()

            snapshot.documents
                .mapNotNull { it.getString("subject") }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllGroups(year: Int, month: Int): List<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val collectionName = "attendance_${year}_${month.toString().padStart(2, '0')}"
            val snapshot = firestore!!.collection(collectionName)
                .select("group")
                .limit(1000)
                .get()
                .get()

            snapshot.documents
                .mapNotNull { it.getString("group") }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            emptyList()
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
}

// Simple data class for parallel loading
data class DashboardData(
    val records: List<AttendanceRecord>,
    val subjects: List<String>,
    val groups: List<String>,
    val subjectTotals: Map<String, SubjectTotals>
)

// Total classes for each subject from subjects collection
data class SubjectTotals(
    val lectTotal: Int = 0,
    val labTotal: Int = 0,
    val tutTotal: Int = 0
) {
    val totalClasses: Int get() = lectTotal + labTotal + tutTotal
}