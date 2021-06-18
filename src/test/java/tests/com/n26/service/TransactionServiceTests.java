package tests.com.n26.service;

import com.n26.dto.StatisticsDto;
import com.n26.dto.TransactionDto;
import com.n26.exception.FutureDateValidationException;
import com.n26.exception.NotWithInRangeValidationException;
import com.n26.model.Transaction;
import com.n26.service.TransactionServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class TransactionServiceTests {
    @Mock
    private TransactionServiceImpl transactionServiceMock = new TransactionServiceImpl();
    @InjectMocks
    private TransactionServiceImpl transactionService;
    @Mock
    private ConcurrentHashMap<Long, List<Transaction>> transactionStore;
    @Test
    public void createTransaction(){
        LocalDateTime timeStamp = LocalDateTime.now().minusSeconds(30);
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), timeStamp);

        Mockito.when(transactionService.addTransaction(dto)).thenReturn(true);

        assertTrue(transactionService.addTransaction(dto));

    }

    @Test(expected = FutureDateValidationException.class)
    public void futureDateValidationException(){
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), LocalDateTime.now().plusSeconds(100));

        Mockito.when(transactionService.addTransaction(dto)).thenThrow(FutureDateValidationException.class);
        transactionService.addTransaction(dto);

        Mockito.verify(transactionService, Mockito.times(1)).addTransaction(dto);
    }

    @Test(expected = NotWithInRangeValidationException.class)
    public void notWithInRangeValidationException(){
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), LocalDateTime.now().minusSeconds(100));

        Mockito.when(transactionService.addTransaction(dto)).thenThrow(NotWithInRangeValidationException.class);
        transactionService.addTransaction(dto);
        Mockito.verify(transactionService, Mockito.times(1)).addTransaction(dto);
    }

    @Test
    public void createMultipleTransaction(){
        AtomicReference<TransactionDto> dto = new AtomicReference<>();
        IntStream.range(0,3).forEach(count->{
            dto.set(new TransactionDto(new BigDecimal(100.00), LocalDateTime.now().minusSeconds(1)));
            Mockito.when(transactionService.addTransaction(dto.get())).thenReturn(true);
            transactionService.addTransaction(dto.get());
        });
    }

    @Test
    public void validateThatIntervalIsGreaterThanZero(){
        assertThat(transactionService.getInterval(),is(60));
    }

    @Test
    public void validateDeleteAllTransactionClears(){
        LocalDateTime timeStamp = LocalDateTime.now().minusSeconds(30);
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), timeStamp);

        Mockito.when(transactionService.addTransaction(dto)).thenReturn(true);

        assertTrue(transactionService.addTransaction(dto));
        assertTrue(transactionService.deleteAllTransactions());
    }

    @Test
    public void statistics(){
        AtomicReference<TransactionDto> dto = new AtomicReference<>();

        StatisticsDto statisticsDto = new StatisticsDto(new BigDecimal(100.00).setScale(2),new BigDecimal(100.00).setScale(2),new BigDecimal(100.00).setScale(2),new BigDecimal(100.00).setScale(2), 3);
        Mockito.when(transactionServiceMock.getStatistics()).thenReturn(statisticsDto);

        IntStream.range(0,3).forEach(count->{
            dto.set(new TransactionDto(new BigDecimal(100.00), LocalDateTime.now().minusSeconds(1)));
            Mockito.when(transactionServiceMock.addTransaction(dto.get())).thenReturn(true);
            transactionServiceMock.addTransaction(dto.get());
        });

        StatisticsDto response = transactionServiceMock.getStatistics();

        assertThat(response.getCount(),is(statisticsDto.getCount()));
        assertThat(response.getMax(),is(statisticsDto.getMax()));
        assertThat(response.getMin(),is(statisticsDto.getMin()));
        assertThat(response.getAvg(),is(statisticsDto.getAvg()));
        assertThat(response.getSum(),is(statisticsDto.getSum()));
        assertTrue(response.getMax().scale() == 2);
        assertTrue(response.getMin().scale() == 2);
        assertTrue(response.getAvg().scale() == 2);
        assertTrue(response.getSum().scale() == 2);
    }

    @Test
    public void getTransactionSize(){
        LocalDateTime timeStamp = LocalDateTime.now().minusSeconds(30);
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), timeStamp);

        Mockito.when(transactionServiceMock.addTransaction(dto)).thenReturn(true);
        Mockito.when(transactionServiceMock.getTransactionSize()).thenReturn(1L);

        assertTrue(transactionServiceMock.addTransaction(dto));
        assertThat(transactionServiceMock.getTransactionSize(),is(1L));

        Mockito.verify(transactionServiceMock, Mockito.times(1)).getTransactionSize();

    }
}
