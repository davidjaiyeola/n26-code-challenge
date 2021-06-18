package tests.com.n26.controller;

import com.n26.controllers.TransactionController;
import com.n26.dto.StatisticsDto;
import com.n26.dto.TransactionDto;
import com.n26.exception.FutureDateValidationException;
import com.n26.exception.NotWithInRangeValidationException;
import com.n26.service.TransactionServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class TransactionControllerUnitTests {

    @InjectMocks
    private TransactionController transactionController;
    @Mock
    private TransactionServiceImpl transactionService ;

    @Test(expected = FutureDateValidationException.class)
    public void futureDateValidationException_return_422(){
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), LocalDateTime.now().plusSeconds(100));

        Mockito.when(transactionService.addTransaction(dto)).thenThrow(FutureDateValidationException.class);
        ResponseEntity responseEntity = transactionController.addTransaction(dto);
        assertThat(responseEntity.getStatusCode(),is(HttpStatus.UNPROCESSABLE_ENTITY));
        Mockito.verify(transactionService, Mockito.times(1)).addTransaction(dto);
    }

    @Test(expected = NotWithInRangeValidationException.class)
    public void notWithInRangeValidationException_return_204(){
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), LocalDateTime.now().plusSeconds(100));

        Mockito.when(transactionService.addTransaction(dto)).thenThrow(NotWithInRangeValidationException.class);
        ResponseEntity responseEntity = transactionController.addTransaction(dto);
        assertThat(responseEntity.getStatusCode(),is(HttpStatus.NO_CONTENT));
        Mockito.verify(transactionService, Mockito.times(1)).addTransaction(dto);
    }


    //Test create
    @Test
    public void createTransaction(){
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), LocalDateTime.now().minusSeconds(4));
        Mockito.when(transactionService.addTransaction(dto)).thenReturn(true);

        ResponseEntity responseEntity = transactionController.addTransaction(dto);
        assertThat(responseEntity.getStatusCode(),is(HttpStatus.CREATED));

        Mockito.verify(transactionService, Mockito.times(1)).addTransaction(dto);
    }

    //Test Delete all
    @Test
    public void deleteAllTransactions(){
        AtomicReference<TransactionDto> dto = new AtomicReference<>();
        IntStream.range(0,3).forEach(count->{
            dto.set(new TransactionDto(new BigDecimal(100.00), LocalDateTime.now().minusSeconds(1)));
            Mockito.when(transactionService.addTransaction(dto.get())).thenReturn(true);
            transactionService.addTransaction(dto.get());
        });

        Mockito.when(transactionService.deleteAllTransactions()).thenReturn(true);

        ResponseEntity responseEntity = transactionController.deleteAllTransactions();
        assertThat(responseEntity.getStatusCode(),is(HttpStatus.NO_CONTENT));

        Mockito.verify(transactionService, Mockito.times(1)).deleteAllTransactions();
    }

    @Test
    public void deleteAllTransactions_failure(){
        AtomicReference<TransactionDto> dto = new AtomicReference<>();
        IntStream.range(0,3).forEach(count->{
            dto.set(new TransactionDto(new BigDecimal(100.00), LocalDateTime.now().minusSeconds(1)));
            Mockito.when(transactionService.addTransaction(dto.get())).thenReturn(true);
            transactionService.addTransaction(dto.get());
        });

        Mockito.when(transactionService.deleteAllTransactions()).thenReturn(false);

        ResponseEntity responseEntity = transactionController.deleteAllTransactions();
        assertThat(responseEntity.getStatusCode(),is(HttpStatus.INTERNAL_SERVER_ERROR));

        Mockito.verify(transactionService, Mockito.times(1)).deleteAllTransactions();
    }

    //Test get stats
    @Test
    public void statistics(){
        AtomicReference<TransactionDto> dto = new AtomicReference<>();

        StatisticsDto statisticsDto = new StatisticsDto(new BigDecimal(100.00).setScale(2),new BigDecimal(100.00).setScale(2),new BigDecimal(100.00).setScale(2),new BigDecimal(100.00).setScale(2), 3);
        Mockito.when(transactionService.getStatistics()).thenReturn(statisticsDto);

        IntStream.range(0,3).forEach(count->{
            dto.set(new TransactionDto(new BigDecimal(100.00), LocalDateTime.now().minusSeconds(1)));
            Mockito.when(transactionService.addTransaction(dto.get())).thenReturn(true);
            transactionService.addTransaction(dto.get());
        });

        StatisticsDto responseEntity = transactionController.statistics();
        assertThat(responseEntity,is(statisticsDto));

        assertThat(responseEntity.getCount(),is(statisticsDto.getCount()));
        assertThat(responseEntity.getMax(),is(statisticsDto.getMax()));
        assertThat(responseEntity.getMin(),is(statisticsDto.getMin()));
        assertThat(responseEntity.getAvg(),is(statisticsDto.getAvg()));
        assertThat(responseEntity.getSum(),is(statisticsDto.getSum()));
        assertTrue(responseEntity.getMax().scale() == 2);
        assertTrue(responseEntity.getMin().scale() == 2);
        assertTrue(responseEntity.getAvg().scale() == 2);
        assertTrue(responseEntity.getSum().scale() == 2);
    }


}
