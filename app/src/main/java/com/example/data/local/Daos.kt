package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUser(userId: String): UserProfile?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun observeUser(userId: String): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE userId = :userId ORDER BY name ASC")
    fun observeCustomers(userId: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE customerId = :customerId")
    suspend fun getCustomerById(customerId: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE customerId = :customerId")
    suspend fun deleteCustomerById(customerId: String)
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services WHERE userId = :userId ORDER BY name ASC")
    fun observeServices(userId: String): Flow<List<ServiceItem>>

    @Query("SELECT * FROM services WHERE userId = :userId ORDER BY name ASC")
    suspend fun getServicesList(userId: String): List<ServiceItem>

    @Query("SELECT * FROM services WHERE serviceId = :serviceId")
    suspend fun getServiceById(serviceId: String): ServiceItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceItem)

    @Delete
    suspend fun deleteService(service: ServiceItem)
}

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeJobs(userId: String): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE jobId = :jobId")
    suspend fun getJobById(jobId: String): Job?

    @Query("SELECT * FROM jobs WHERE userId = :userId AND customerId = :customerId ORDER BY createdAt DESC")
    fun observeJobsForCustomer(userId: String, customerId: String): Flow<List<Job>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: Job)

    @Delete
    suspend fun deleteJob(job: Job)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY createdAt DESC")
    fun observePayments(userId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE userId = :userId AND customerId = :customerId ORDER BY createdAt DESC")
    fun observePaymentsForCustomer(userId: String, customerId: String): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeExpenses(userId: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}
