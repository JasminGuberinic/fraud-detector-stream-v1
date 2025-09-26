package com.virtualcode.fraud.infrastructure.persistence.repository

import com.virtualcode.fraud.infrastructure.persistence.entity.ProcessedTransactionEntity
import com.virtualcode.fraud.infrastructure.persistence.entity.TransactionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : JpaRepository<TransactionEntity, String>

@Repository
interface ProcessedTransactionRepository : JpaRepository<ProcessedTransactionEntity, String>