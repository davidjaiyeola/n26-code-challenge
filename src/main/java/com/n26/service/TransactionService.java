package com.n26.service;

import com.n26.dto.StatisticsDto;
import com.n26.dto.TransactionDto;
import com.n26.exception.FutureDateValidationException;
import com.n26.exception.NotWithInRangeValidationException;
import com.n26.model.Transaction;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public interface TransactionService {
    StatisticsDto getStatistics();
    boolean addTransaction(TransactionDto transactionDto) throws NotWithInRangeValidationException, FutureDateValidationException;
    boolean deleteAllTransactions();
    ConcurrentHashMap<Long, List<Transaction>> getTransactionStore();
    int getInterval();
    long getTransactionSize();
}
