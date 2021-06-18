package com.n26.service;

import com.n26.dto.StatisticsDto;
import com.n26.dto.TransactionDto;
import com.n26.exception.FutureDateValidationException;
import com.n26.exception.NotWithInRangeValidationException;
import com.n26.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TransactionServiceImpl implements TransactionService{
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);
    @Value("${n26.statistics-interval}")
    protected int interval;

    //ConcurrentHashMap for thread safety
    private ConcurrentHashMap<Long, List<Transaction>> transactionStore;

    public TransactionServiceImpl(){
        transactionStore = new ConcurrentHashMap<>();
        if(interval == 0)
            interval = 60;
    }

    @Override
    public ConcurrentHashMap<Long, List<Transaction>> getTransactionStore(){
        return this.transactionStore;
    }

    @Override
    public int getInterval() {
        return this.interval;
    }

    @Override
    public long getTransactionSize() {
        return  transactionStore.values().stream()
                .flatMap(list -> list.stream())
                .count();

    }

    @Override
    public StatisticsDto getStatistics() {
            //Now
            long endTime = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
            //interval seconds ago
            long startTime = endTime - (interval * 1000);
            long requestTimeStampSeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            List<Transaction> transactionsWithInInterval = new ArrayList<>();
            //We use range instead of traditional for-loop
            IntStream.rangeClosed(0, interval).forEach(count -> {
                List<Transaction> existing = transactionStore.get(requestTimeStampSeconds - count);
                if (existing != null) {
                    //We futher filter as some might have expired based on milliseconds
                    transactionsWithInInterval.addAll(existing.stream()
                            .filter(transaction -> {
                                long transactionTimeMills = transaction.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli();
                                return (transactionTimeMills <= endTime) && (transactionTimeMills >= startTime);
                            }).collect(Collectors.toList()));
                }
            });

            //Luckily we have stats utility in java util
            if (transactionsWithInInterval.size() > 0) {
                DoubleSummaryStatistics doubleSummaryStatistics = transactionsWithInInterval.stream()
                        .collect(Collectors.summarizingDouble(t -> t.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
                return new StatisticsDto(
                        new BigDecimal(doubleSummaryStatistics.getSum()).setScale(2, BigDecimal.ROUND_HALF_UP),
                        new BigDecimal(doubleSummaryStatistics.getAverage()).setScale(2, BigDecimal.ROUND_HALF_UP),
                        new BigDecimal(doubleSummaryStatistics.getMax()).setScale(2, BigDecimal.ROUND_HALF_UP),
                        new BigDecimal(doubleSummaryStatistics.getMin()).setScale(2, BigDecimal.ROUND_HALF_UP),
                        doubleSummaryStatistics.getCount());
            } else {
                return new StatisticsDto(new BigDecimal(0.00).setScale(2), new BigDecimal(0.00).setScale(2), new BigDecimal(0.00).setScale(2), new BigDecimal(0.00).setScale(2), 0);
            }
    }

    @Override
    public boolean addTransaction(TransactionDto transactionDto) throws NotWithInRangeValidationException, FutureDateValidationException {
        final Transaction transaction = new Transaction(transactionDto.getAmount(), transactionDto.getTimestamp());

        long timeStampInSeconds = transaction.getTimestamp().toEpochSecond(ZoneOffset.UTC);
        long differenceInSeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - timeStampInSeconds;

        //Date in the future
        if( differenceInSeconds < 0 ){
            throw new FutureDateValidationException("TimeStamp cannot be greater than current time");
        }else if(differenceInSeconds >= interval){
            throw new NotWithInRangeValidationException("TimeStamp must be within last "+interval+"s");
        }

        List<Transaction> existing = transactionStore.get(timeStampInSeconds);
        if(existing == null){
            existing = new ArrayList<>();
        }

        existing.add(transaction);
        transactionStore.put(timeStampInSeconds, existing);
        log.info("Transaction added successfully");
        return true;
    }

    @Override
    public boolean deleteAllTransactions() {
        transactionStore.clear();
        return true;
    }
}
