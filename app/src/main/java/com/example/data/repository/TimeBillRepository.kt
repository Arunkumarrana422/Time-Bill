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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: "local_offline_user"
    }

    // User Profile
    fun observeUserProfile(userId: String): Flow<UserProfile?> = appDb.userDao().observeUser(userId)

    suspend fun saveUserProfile(profile: UserProfile) {
        withContext(Dispatchers.IO) {
            appDb.userDao().insertUser(profile)
            val uid = auth.currentUser?.uid ?: profile.userId
            if (uid.isNotEmpty() && uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).set(profile).addOnFailureListener {
                        Log.e("Repo", "Failed to sync user profile to Firestore", it)
                    }
                } catch (e: Exception) {
                    Log.e("Repo", "Firestore sync exception", e)
                }
            }
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
                } catch (e: Exception) {
                    Log.e("Repo", "Service sync exception", e)
                }
            }
        }
    }

    suspend fun deleteService(service: ServiceItem) {
        withContext(Dispatchers.IO) {
            appDb.serviceDao().deleteService(service)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("services").document(service.serviceId)
                        .delete()
                } catch (e: Exception) {
                    Log.e("Repo", "Delete service exception", e)
                }
            }
        }
    }

    // Jobs
    fun observeJobs(userId: String): Flow<List<Job>> = appDb.jobDao().observeJobs(userId)

    suspend fun saveJob(job: Job) {
        withContext(Dispatchers.IO) {
            appDb.jobDao().insertJob(job)
            val uid = requireUserId()
            if (uid != "local_offline_user") {
                try {
                    db.collection("users").document(uid).collection("jobs").document(job.jobId)
                        .set(job)
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
                    ServiceItem(serviceId = "srv_1_$userId", userId = userId, name = "Ploughing / जुताई", description = "Tractor field ploughing", hourlyRate = 600.0, minimumCharge = 300.0),
                    ServiceItem(serviceId = "srv_2_$userId", userId = userId, name = "Rotavator / रोटावेटर", description = "Fine soil preparation", hourlyRate = 800.0, minimumCharge = 400.0),
                    ServiceItem(serviceId = "srv_3_$userId", userId = userId, name = "Cultivator / कल्टीवेटर", description = "Soil loosening & weed removal", hourlyRate = 500.0, minimumCharge = 250.0),
                    ServiceItem(serviceId = "srv_4_$userId", userId = userId, name = "Harvester / कटाई", description = "Crop harvesting", hourlyRate = 1200.0, minimumCharge = 600.0),
                    ServiceItem(serviceId = "srv_5_$userId", userId = userId, name = "Thresher / थ्रेशर", description = "Grain separation", hourlyRate = 700.0, minimumCharge = 350.0),
                    ServiceItem(serviceId = "srv_6_$userId", userId = userId, name = "Trolley Transport / ट्रॉली ढुलाई", description = "Goods & crop transportation", hourlyRate = 500.0, minimumCharge = 250.0),
                    ServiceItem(serviceId = "srv_7_$userId", userId = userId, name = "Spraying / कीटनाशक छिड़काव", description = "Pesticide & fertilizer spray", hourlyRate = 400.0, minimumCharge = 200.0),
                    ServiceItem(serviceId = "srv_8_$userId", userId = userId, name = "General Work / सामान्य कार्य", description = "General hourly machinery work", hourlyRate = 500.0, minimumCharge = 250.0)
                )
                defaultServices.forEach { service ->
                    saveService(service)
                }
            }
        }
    }
}
