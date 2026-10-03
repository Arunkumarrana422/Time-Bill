package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey val userId: String,
    val name: String = "",
    val businessName: String = "",
    val mobile: String = "",
    val address: String = "",
    val currency: String = "₹",
    val defaultRate: Double = 500.0,
    val defaultService: String = "Tractor Ploughing",
    val invoicePrefix: String = "INV",
    val paymentTerms: String = "Due on receipt",
    val isSetupComplete: Boolean = false
) : Serializable

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val customerId: String,
    val userId: String = "",
    val name: String,
    val mobile: String,
    val address: String = "",
    val village: String = "",
    val notes: String = "",
    val totalJobs: Int = 0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "services")
data class ServiceItem(
    @PrimaryKey val serviceId: String,
    val userId: String = "",
    val name: String,
    val description: String = "",
    val hourlyRate: Double = 500.0,
    val minuteRate: Double = 8.33,
    val minimumCharge: Double = 100.0,
    val isActive: Boolean = true
) : Serializable

@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey val jobId: String,
    val userId: String = "",
    val customerId: String,
    val customerName: String,
    val serviceId: String,
    val serviceName: String,
    val date: String, // yyyy-MM-dd
    val startTime: String,
    val endTime: String,
    val totalDurationMinutes: Int,
    val breakDurationMinutes: Int,
    val billableDurationMinutes: Int,
    val rate: Double,
    val baseAmount: Double,
    val additionalChargesAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val finalAmount: Double,
    val paidAmount: Double = 0.0,
    val pendingAmount: Double,
    val paymentStatus: String = "Pending", // Pending, Partially Paid, Paid
    val status: String = "Completed", // Running, Completed, Cancelled
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey val paymentId: String,
    val userId: String = "",
    val customerId: String,
    val customerName: String = "",
    val jobId: String = "",
    val amount: Double,
    val method: String = "Cash", // Cash, UPI, Bank Transfer, Other
    val date: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val expenseId: String,
    val userId: String = "",
    val name: String,
    val category: String = "Fuel", // Fuel, Maintenance, Labour, Transport, Repair, Machine, Other
    val amount: Double,
    val date: String,
    val description: String = "",
    val paymentMethod: String = "Cash",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

data class AdditionalCharge(
    val name: String,
    val quantity: Double,
    val rate: Double,
    val amount: Double
) : Serializable
