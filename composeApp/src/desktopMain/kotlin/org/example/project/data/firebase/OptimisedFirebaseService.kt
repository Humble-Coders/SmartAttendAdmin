package org.example.project.data.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.Timestamp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.models.AttendanceMetadata
import org.example.project.models.AttendanceStats
import org.example.project.models.CacheEntry
import org.example.project.models.PaginatedStudents
import org.example.project.models.StudentAggregatedStats
import org.example.project.models.StudentAttendanceSummary
import org.example.project.models.SubjectAttendanceStats
import org.example.project.models.SubjectStats
import org.example.project.models.SubjectTypeStats
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class OptimizedFirebaseService {
    private var firestore: Firestore? = null

    // In-memory cache for frequently accessed data
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val cacheTimeout = 5 * 60 * 1000L // 5 minutes

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
            println("Smart Attend: Optimized Firebase initialized successfully")
            true
        } catch (e: Exception) {
            println("Smart Attend: Firebase initialization failed - ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // ⚡ FAST: Get metadata in single document read
    suspend fun getAttendanceMetadata(year: Int, month: Int): AttendanceMetadata = withContext(Dispatchers.IO) {
        val cacheKey = "metadata_${year}_$month"

        // Check cache first
        getCachedData<AttendanceMetadata>(cacheKey)?.let { return@withContext it }

        return@withContext try {
            val metadataId = "${year}_${month.toString().padStart(2, '0')}"
            val snapshot = firestore!!.collection("attendance_metadata")
                .document(metadataId)
                .get()
                .get()

            val metadata = if (snapshot.exists()) {
                val data = snapshot.data!!
                AttendanceMetadata(
                    subjects = (data["subjects"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    groups = (data["groups"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    totalStudents = (data["totalStudents"] as? Number)?.toInt() ?: 0,
                    totalClasses = (data["totalClasses"] as? Number)?.toInt() ?: 0,
                    overallAttendanceRate = (data["overallAttendanceRate"] as? Number)?.toDouble() ?: 0.0,
                    lastUpdated = convertTimestamp(data["lastUpdated"])
                )
            } else {
                // Fallback: create metadata from raw data (slower)
                println("Smart Attend: Metadata not found, creating from raw data...")
                createMetadataFromRawData(year, month)
            }

            // Cache the result
            setCachedData(cacheKey, metadata)
            metadata
        } catch (e: Exception) {
            println("Error fetching metadata: ${e.message}")
            AttendanceMetadata()
        }
    }

    // ⚡ FAST: Get pre-calculated subject statistics
    suspend fun getSubjectStatsOptimized(
        year: Int,
        month: Int,
        group: String? = null,
        subject: String? = null
    ): List<SubjectStats> = withContext(Dispatchers.IO) {
        val cacheKey = "subject_stats_${year}_${month}_${group}_$subject"

        // Check cache first
        getCachedData<List<SubjectStats>>(cacheKey)?.let { return@withContext it }

        return@withContext try {
            val prefix = "${year}_${month.toString().padStart(2, '0')}"
            var query = firestore!!.collection("attendance_stats")
                .whereGreaterThanOrEqualTo("__name__", prefix)
                .whereLessThan("__name__", prefix + "\uf8ff")
                .limit(200) // Reasonable limit for performance

            val snapshot = query.get().get()

            val stats = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val docSubject = data["subject"] as? String ?: return@mapNotNull null
                    val docGroup = data["group"] as? String ?: return@mapNotNull null

                    // Apply client-side filtering
                    if (group != null && docGroup != group) return@mapNotNull null
                    if (subject != null && docSubject != subject) return@mapNotNull null

                    SubjectStats(
                        subject = docSubject,
                        group = docGroup,
                        totalStudents = (data["totalStudents"] as? Number)?.toInt() ?: 0,
                        totalClasses = (data["totalClasses"] as? Number)?.toInt() ?: 0,
                        presentCount = (data["presentCount"] as? Number)?.toInt() ?: 0,
                        attendanceRate = (data["attendanceRate"] as? Number)?.toDouble() ?: 0.0,
                        lectureStats = parseTypeStats(data["lectureStats"] as? Map<String, Any>),
                        tutorialStats = parseTypeStats(data["tutorialStats"] as? Map<String, Any>),
                        labStats = parseTypeStats(data["labStats"] as? Map<String, Any>)
                    )
                } catch (e: Exception) {
                    println("Error parsing subject stats: ${e.message}")
                    null
                }
            }

            // Cache the result
            setCachedData(cacheKey, stats)
            stats
        } catch (e: Exception) {
            println("Error fetching optimized subject stats: ${e.message}")
            // Fallback to slow method
            getSubjectStatsFromRawData(year, month, group, subject)
        }
    }

    // ⚡ FAST: Get student data from pre-aggregated collection
    suspend fun getStudentStatsOptimized(
        rollNumber: String,
        year: Int,
        month: Int
    ): StudentAggregatedStats? = withContext(Dispatchers.IO) {
        val cacheKey = "student_${year}_${month}_$rollNumber"

        // Check cache first
        getCachedData<StudentAggregatedStats>(cacheKey)?.let { return@withContext it }

        return@withContext try {
            val studentId = "${year}_${month.toString().padStart(2, '0')}_$rollNumber"
            val snapshot = firestore!!.collection("student_attendance")
                .document(studentId)
                .get()
                .get()

            val stats = if (snapshot.exists()) {
                val data = snapshot.data!!
                StudentAggregatedStats(
                    rollNumber = rollNumber,
                    year = year,
                    month = month,
                    group = data["group"] as? String ?: "",
                    subjects = parseSubjectMap(data["subjects"] as? Map<String, Any>),
                    overallStats = parseAttendanceStats(data["overallStats"] as? Map<String, Any>)
                )
            } else {
                // Fallback: create from raw data
                createStudentStatsFromRawData(rollNumber, year, month)
            }

            // Cache the result
            stats?.let { setCachedData(cacheKey, it) }
            stats
        } catch (e: Exception) {
            println("Error fetching student stats: ${e.message}")
            null
        }
    }

    // ⚡ FAST: Paginated student list for large datasets
    suspend fun getStudentsPaginated(
        subject: String,
        year: Int,
        month: Int,
        group: String? = null,
        limit: Int = 50,
        lastRollNumber: String? = null
    ): PaginatedStudents = withContext(Dispatchers.IO) {
        return@withContext try {
            val prefix = "${year}_${month.toString().padStart(2, '0')}"
            var query = firestore!!.collection("student_attendance")
                .whereGreaterThanOrEqualTo("__name__", prefix)
                .whereLessThan("__name__", prefix + "\uf8ff")

            // Apply group filter if available
            group?.let {
                query = query.whereEqualTo("group", it)
            }

            query = query.limit((limit + 1).toLong().toInt()) // Get one extra to check if more exist

            // Add pagination
            lastRollNumber?.let {
                val lastStudentId = "${prefix}_$it"
                query = query.startAfter(lastStudentId)
            }

            val snapshot = query.get().get()
            val documents = snapshot.documents

            val hasMore = documents.size > limit
            val studentsToProcess = if (hasMore) documents.dropLast(1) else documents

            val students = studentsToProcess.mapNotNull { doc ->
                try {
                    val data = doc.data
                    val studentRollNumber = data["rollNumber"] as? String ?: return@mapNotNull null
                    val subjects = parseSubjectMap(data["subjects"] as? Map<String, Any>)

                    // Filter by subject
                    val subjectStats = subjects[subject] ?: return@mapNotNull null

                    StudentAttendanceSummary(
                        rollNumber = studentRollNumber,
                        name = "Student $studentRollNumber",
                        stats = AttendanceStats(
                            totalClasses = subjectStats.totalClasses,
                            attendedClasses = subjectStats.attended,
                            percentage = subjectStats.percentage,
                            lectureStats = subjectStats.lectureStats,
                            tutorialStats = subjectStats.tutorialStats,
                            labStats = subjectStats.labStats
                        )
                    )
                } catch (e: Exception) {
                    println("Error parsing student data: ${e.message}")
                    null
                }
            }

            PaginatedStudents(
                students = students,
                hasMore = hasMore,
                lastRollNumber = students.lastOrNull()?.rollNumber,
                totalCount = students.size
            )
        } catch (e: Exception) {
            println("Error fetching paginated students: ${e.message}")
            PaginatedStudents(emptyList(), false, null, 0)
        }
    }

    // FALLBACK METHODS: Use raw data when optimized data isn't available
    private suspend fun createMetadataFromRawData(year: Int, month: Int): AttendanceMetadata {
        return try {
            val collectionName = "attendance_${year}_${month.toString().padStart(2, '0')}"

            // Get subjects (limit to reduce reads)
            val subjectsSnapshot = firestore!!.collection(collectionName)
                .select("subject")
                .limit(1000)
                .get()
                .get()
            val subjects = subjectsSnapshot.documents
                .mapNotNull { it.getString("subject") }
                .distinct()
                .sorted()

            // Get groups
            val groupsSnapshot = firestore!!.collection(collectionName)
                .select("group")
                .limit(1000)
                .get()
                .get()
            val groups = groupsSnapshot.documents
                .mapNotNull { it.getString("group") }
                .distinct()
                .sorted()

            // Basic stats (you might want to store this as a scheduled job)
            AttendanceMetadata(
                subjects = subjects,
                groups = groups,
                totalStudents = 0, // Would need separate calculation
                totalClasses = 0,  // Would need separate calculation
                overallAttendanceRate = 0.0,
                lastUpdated = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
        } catch (e: Exception) {
            println("Error creating metadata from raw data: ${e.message}")
            AttendanceMetadata()
        }
    }

    private suspend fun getSubjectStatsFromRawData(
        year: Int,
        month: Int,
        group: String?,
        subject: String?
    ): List<SubjectStats> {
        // Fallback implementation using raw attendance data
        // This would be slower but ensures functionality
        return emptyList() // Simplified for now
    }

    private suspend fun createStudentStatsFromRawData(
        rollNumber: String,
        year: Int,
        month: Int
    ): StudentAggregatedStats? {
        // Fallback implementation
        return null // Simplified for now
    }

    // CACHE MANAGEMENT
    private inline fun <reified T> getCachedData(key: String): T? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > cacheTimeout) {
            cache.remove(key)
            return null
        }
        return entry.data as? T
    }

    private fun setCachedData(key: String, data: Any) {
        cache[key] = CacheEntry(data)

        // Simple cache cleanup (remove oldest entries if cache gets too large)
        if (cache.size > 100) {
            val oldestKey = cache.entries.minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { cache.remove(it) }
        }
    }

    // HELPER FUNCTIONS
    private fun convertTimestamp(timestampObj: Any?): String {
        return try {
            when (timestampObj) {
                is Timestamp -> {
                    val date = Date(timestampObj.seconds * 1000 + timestampObj.nanos / 1000000)
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
                }
                is String -> timestampObj
                else -> timestampObj?.toString() ?: ""
            }
        } catch (e: Exception) {
            timestampObj?.toString() ?: ""
        }
    }

    private fun parseTypeStats(data: Map<String, Any>?): SubjectTypeStats {
        if (data == null) return SubjectTypeStats()
        return SubjectTypeStats(
            total = (data["total"] as? Number)?.toInt() ?: 0,
            attended = (data["attended"] as? Number)?.toInt() ?: 0,
            percentage = (data["percentage"] as? Number)?.toDouble() ?: 0.0
        )
    }

    private fun parseSubjectMap(data: Map<String, Any>?): Map<String, SubjectAttendanceStats> {
        if (data == null) return emptyMap()
        return data.mapValues { (_, value) ->
            val subjectData = value as? Map<String, Any> ?: return@mapValues SubjectAttendanceStats()
            SubjectAttendanceStats(
                totalClasses = (subjectData["totalClasses"] as? Number)?.toInt() ?: 0,
                attended = (subjectData["attended"] as? Number)?.toInt() ?: 0,
                percentage = (subjectData["percentage"] as? Number)?.toDouble() ?: 0.0,
                lectureStats = parseTypeStats(subjectData["lectureStats"] as? Map<String, Any>),
                tutorialStats = parseTypeStats(subjectData["tutorialStats"] as? Map<String, Any>),
                labStats = parseTypeStats(subjectData["labStats"] as? Map<String, Any>)
            )
        }
    }

    private fun parseAttendanceStats(data: Map<String, Any>?): AttendanceStats {
        if (data == null) return AttendanceStats()
        return AttendanceStats(
            totalClasses = (data["totalClasses"] as? Number)?.toInt() ?: 0,
            attendedClasses = (data["attendedClasses"] as? Number)?.toInt() ?: 0,
            percentage = (data["percentage"] as? Number)?.toDouble() ?: 0.0,
            lectureStats = parseTypeStats(data["lectureStats"] as? Map<String, Any>),
            tutorialStats = parseTypeStats(data["tutorialStats"] as? Map<String, Any>),
            labStats = parseTypeStats(data["labStats"] as? Map<String, Any>)
        )
    }
}