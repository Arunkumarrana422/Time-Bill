package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TimeBillRepository(private val context: Context) {
    private val db: FirebaseFirestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("Repo", "Failed to get Firestore instance", e)
            FirebaseFirestore.getInstance()
        }
    }
    private val appDb = AppDatabase.getDatabase(context)
    private val auth by lazy { Firebase.auth }
    private var profileListener: ListenerRegistration? = null

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: "local_offline_user"
    }

    // User Profile
    fun observeUserProfile(userId: String): Flow<UserProfile?> = appDb.userDao().observeUser(userId)

    suspend fun getUser(userId: String): UserProfile? {
        return withContext(Dispatchers.IO) {
            appDb.userDao().getUser(userId)
        }
    }

    suspend fun fetchAndCacheUserProfile(userId: String): UserProfile? {
        if (userId.isEmpty() || userId == "local_offline_user") return null
        return withContext(Dispatchers.IO) {
            try {
                val doc = db.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        appDb.userDao().insertUser(profile)
                        return@withContext profile
                    }
                }
            } catch (e: Exception) {
                Log.e("Repo", "fetchAndCacheUserProfile error: ${e.message}")
            }
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        withContext(Dispatchers.IO) {
            val uid = if (profile.userId.isNotBlank()) profile.userId else auth.currentUser?.uid ?: "local_offline_user"
            val cleanProfile = profile.copy(userId = uid)
            appDb.userDao().insertUser(cleanProfile)
            if (uid.isNotEmpty() && uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).set(cleanProfile).await()
                    Log.d("Repo", "User profile synced successfully to Firestore for $uid")
                } catch (e: Exception) {
                    Log.e("Repo", "Firestore sync exception for user profile", e)
                    try {
                        db.collection("users").document(uid).set(cleanProfile)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun startRealtimeProfileListener(userId: String, scope: CoroutineScope) {
        if (userId.isEmpty() || userId == "local_offline_user") return
        profileListener?.remove()
        try {
            profileListener = db.collection("users").document(userId).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Repo", "Realtime user profile listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    if (profile != null) {
                        scope.launch(Dispatchers.IO) {
                            appDb.userDao().insertUser(profile)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Repo", "Failed to start user profile listener", e)
        }
    }

    // Customers
    fun observeCustomers(userId: String): Flow<List<Customer>> = appDb.customerDao().observeCustomers(userId)

    suspend fun saveCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            appDb.customerDao().insertCustomer(customer)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("customers").document(customer.customerId)
                        .set(customer)
                        .addOnFailureListener { Log.e("Repo", "Sync customer failed", it) }
                } catch (e: Exception) {
                    Log.e("Repo", "Customer sync exception", e)
                }
            }
        }
    }

    suspend fun deleteCustomer(customer: Customer) {
        withContext(Dispatchers.IO) {
            appDb.customerDao().deleteCustomer(customer)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("customers").document(customer.customerId)
                        .delete()
                } catch (e: Exception) {
                    Log.e("Repo", "Delete customer exception", e)
                }
            }
        }
    }

    // Services
    fun observeServices(userId: String): Flow<List<ServiceItem>> = appDb.serviceDao().observeServices(userId)

    suspend fun saveService(service: ServiceItem) {
        withContext(Dispatchers.IO) {
            appDb.serviceDao().insertService(service)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("services").document(service.serviceId)
                        .set(service)
                        .addOnFailureListener { Log.e("Repo", "Sync service failed", it) }
                } catch (e: Exception) {
                    Log.e("Repo", "Service sync exception", e)
                }
            }
        }
    }

    // Jobs
    fun observeJobs(userId: String): Flow<List<Job>> = appDb.jobDao().observeJobs(userId)

    suspend fun getJob(jobId: String): Job? {
        return withContext(Dispatchers.IO) {
            appDb.jobDao().getJobById(jobId)
        }
    }

    suspend fun saveJob(job: Job) {
        withContext(Dispatchers.IO) {
            appDb.jobDao().insertJob(job)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("jobs").document(job.jobId)
                        .set(job)
                        .addOnFailureListener { Log.e("Repo", "Sync job failed", it) }
                } catch (e: Exception) {
                    Log.e("Repo", "Job sync exception", e)
                }
            }
        }
    }

    suspend fun deleteJob(job: Job) {
        withContext(Dispatchers.IO) {
            appDb.jobDao().deleteJob(job)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("jobs").document(job.jobId)
                        .delete()
                } catch (e: Exception) {
                    Log.e("Repo", "Delete job exception", e)
                }
            }
        }
    }

    // Payments
    fun observePayments(userId: String): Flow<List<Payment>> = appDb.paymentDao().observePayments(userId)

    suspend fun savePayment(payment: Payment) {
        withContext(Dispatchers.IO) {
            appDb.paymentDao().insertPayment(payment)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("payments").document(payment.paymentId)
                        .set(payment)
                        .addOnFailureListener { Log.e("Repo", "Sync payment failed", it) }
                } catch (e: Exception) {
                    Log.e("Repo", "Payment sync exception", e)
                }
            }
        }
    }

    suspend fun deletePayment(payment: Payment) {
        withContext(Dispatchers.IO) {
            appDb.paymentDao().deletePayment(payment)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("payments").document(payment.paymentId)
                        .delete()
                } catch (e: Exception) {
                    Log.e("Repo", "Delete payment exception", e)
                }
            }
        }
    }

    // Expenses
    fun observeExpenses(userId: String): Flow<List<Expense>> = appDb.expenseDao().observeExpenses(userId)

    suspend fun saveExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            appDb.expenseDao().insertExpense(expense)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("expenses").document(expense.expenseId)
                        .set(expense)
                        .addOnFailureListener { Log.e("Repo", "Sync expense failed", it) }
                } catch (e: Exception) {
                    Log.e("Repo", "Expense sync exception", e)
                }
            }
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            appDb.expenseDao().deleteExpense(expense)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("expenses").document(expense.expenseId)
                        .delete()
                } catch (e: Exception) {
                    Log.e("Repo", "Delete expense exception", e)
                }
            }
        }
    }

    suspend fun seedDefaultServicesIfNeeded(userId: String) {
        withContext(Dispatchers.IO) {
            val existing = appDb.serviceDao().getServicesList(userId)
            if (existing.isEmpty()) {
                val defaultServices = listOf(
                    ServiceItem(serviceId = "srv_1_$userId", userId = userId, name = "Ploughing / जुताई", description = "Tractor field ploughing", hourlyRate = 600.0),
                    ServiceItem(serviceId = "srv_2_$userId", userId = userId, name = "Rotavator / रोटावेटर", description = "Fine soil preparation", hourlyRate = 800.0),
                    ServiceItem(serviceId = "srv_3_$userId", userId = userId, name = "Cultivator / कल्टीवेटर", description = "Soil loosening & weed removal", hourlyRate = 500.0),
                    ServiceItem(serviceId = "srv_4_$userId", userId = userId, name = "Harvester / कटाई", description = "Crop harvesting", hourlyRate = 1200.0),
                    ServiceItem(serviceId = "srv_5_$userId", userId = userId, name = "Thresher / थ्रेशर", description = "Grain separation", hourlyRate = 700.0),
                    ServiceItem(serviceId = "srv_6_$userId", userId = userId, name = "Trolley Transport / ट्रॉली ढुलाई", description = "Goods & crop transportation", hourlyRate = 500.0),
                    ServiceItem(serviceId = "srv_7_$userId", userId = userId, name = "Spraying / कीटनाशक छिड़काव", description = "Pesticide & fertilizer spray", hourlyRate = 400.0),
                    ServiceItem(serviceId = "srv_8_$userId", userId = userId, name = "General Work / सामान्य कार्य", description = "General hourly machinery work", hourlyRate = 500.0)
                )
                defaultServices.forEach { service ->
                    saveService(service)
                }
            }
        }
    }

    suspend fun syncDataFromFirestore(userId: String) {
        if (userId.isEmpty() || userId == "local_offline_user") return
        withContext(Dispatchers.IO) {
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    val profile = userDoc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        appDb.userDao().insertUser(profile)
                    }
                }

                val customersSnap = db.collection("users").document(userId).collection("customers").get().await()
                for (doc in customersSnap.documents) {
                    val customer = doc.toObject(Customer::class.java)
                    if (customer != null) {
                        appDb.customerDao().insertCustomer(customer)
                    }
                }

                val servicesSnap = db.collection("users").document(userId).collection("services").get().await()
                for (doc in servicesSnap.documents) {
                    val service = doc.toObject(ServiceItem::class.java)
                    if (service != null) {
                        appDb.serviceDao().insertService(service)
                    }
                }

                val jobsSnap = db.collection("users").document(userId).collection("jobs").get().await()
                for (doc in jobsSnap.documents) {
                    val job = doc.toObject(Job::class.java)
                    if (job != null) {
                        appDb.jobDao().insertJob(job)
                    }
                }

                val paymentsSnap = db.collection("users").document(userId).collection("payments").get().await()
                for (doc in paymentsSnap.documents) {
                    val payment = doc.toObject(Payment::class.java)
                    if (payment != null) {
                        appDb.paymentDao().insertPayment(payment)
                    }
                }

                val expensesSnap = db.collection("users").document(userId).collection("expenses").get().await()
                for (doc in expensesSnap.documents) {
                    val expense = doc.toObject(Expense::class.java)
                    if (expense != null) {
                        appDb.expenseDao().insertExpense(expense)
                    }
                }
            } catch (e: Exception) {
                Log.e("Repo", "Error syncing data from Firestore: ${e.message}", e)
            }
        }
    }
}
