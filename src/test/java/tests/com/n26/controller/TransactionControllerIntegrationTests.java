package tests.com.n26.controller;

import com.n26.controllers.TransactionController;
import com.n26.dto.StatisticsDto;
import com.n26.dto.TransactionDto;
import com.n26.service.TransactionService;
import com.n26.service.TransactionServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

@EnableAutoConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest(classes= {TransactionController.class, TransactionServiceImpl.class} ,webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionControllerIntegrationTests {
    private static final Logger log = LoggerFactory.getLogger(TransactionControllerIntegrationTests.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private TransactionService transactionService;

    @Test
    public void createTransaction(){
        LocalDateTime timeStamp = LocalDateTime.now().minusSeconds(30);
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), timeStamp);
        HttpEntity<TransactionDto> request = new HttpEntity<>(dto);
        ResponseEntity response = restTemplate.postForEntity("http://127.0.0.1:"+port+"/transactions", request, ResponseEntity.class);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
        assertThat(transactionService.getTransactionSize(), is(1L));

        //Clear]
        ResponseEntity responseDelete = restTemplate.exchange("http://127.0.0.1:"+port+"/transactions", HttpMethod.DELETE,null, ResponseEntity.class);
        assertThat(responseDelete.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
        assertThat(transactionService.getTransactionSize(), is(0L));
    }

    @Test
    public void futureDateValidationException_return_422(){
        LocalDateTime timeStamp = LocalDateTime.now().plusSeconds(100);
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), timeStamp);
        HttpEntity<TransactionDto> request = new HttpEntity<>(dto);
        ResponseEntity response = restTemplate.postForEntity("http://127.0.0.1:"+port+"/transactions", request, ResponseEntity.class);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThat(transactionService.getTransactionSize(), is(0L));

    }

    @Test
    public void notWithInRangeValidationException_return_204(){
        LocalDateTime timeStamp = LocalDateTime.now().minusSeconds(100);
        TransactionDto dto = new TransactionDto(new BigDecimal(100.00), timeStamp);
        HttpEntity<TransactionDto> request = new HttpEntity<>(dto);
        ResponseEntity response = restTemplate.postForEntity("http://127.0.0.1:"+port+"/transactions", request, ResponseEntity.class);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
        assertThat(transactionService.getTransactionSize(), is(0L));

    }

    @Test
    public void multipleTransactionTest(){
        IntStream.range(0,40).forEach(count->{
            LocalDateTime timeStamp = LocalDateTime.now().minusSeconds(count);
            TransactionDto dto = new TransactionDto(new BigDecimal(100.29+ count), timeStamp);
            HttpEntity<TransactionDto> request = new HttpEntity<>(dto);
            ResponseEntity response = restTemplate.postForEntity("http://127.0.0.1:"+port+"/transactions", request, ResponseEntity.class);

            assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
        });

        assertThat(transactionService.getTransactionSize(), is(40L));

        //Clear]
        ResponseEntity responseDelete = restTemplate.exchange("http://127.0.0.1:"+port+"/transactions", HttpMethod.DELETE,null, ResponseEntity.class);
        assertThat(responseDelete.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
        assertThat(transactionService.getTransactionSize(), is(0L));
    }

    @Test
    public void deleteAllTransactions(){
        IntStream.range(0,10).forEach(count->{
            LocalDateTime timeStamp = LocalDateTime.now().minusSeconds(count);
            TransactionDto dto = new TransactionDto(new BigDecimal(100.29+ count), timeStamp);
            HttpEntity<TransactionDto> request = new HttpEntity<>(dto);
            ResponseEntity response = restTemplate.postForEntity("http://127.0.0.1:"+port+"/transactions", request, ResponseEntity.class);

            assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
        });

        ResponseEntity response = restTemplate.exchange("http://127.0.0.1:"+port+"/transactions", HttpMethod.DELETE,null, ResponseEntity.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
        assertThat(transactionService.getTransactionSize(), is(0L));
    }

    //check that stats are to two decimal

    @Test
    public void statistics(){
        IntStream.rangeClosed(1,20).forEach(count->{
            LocalDateTime timeStamp = LocalDateTime.now().minusSeconds(count);
            TransactionDto dto = new TransactionDto(new BigDecimal(100.29+ count), timeStamp);
            HttpEntity<TransactionDto> request = new HttpEntity<>(dto);
            ResponseEntity response = restTemplate.postForEntity("http://127.0.0.1:"+port+"/transactions", request, ResponseEntity.class);

            assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
        });

        assertThat(transactionService.getTransactionSize(), is(20L));

        ResponseEntity<StatisticsDto> response = restTemplate.getForEntity("http://127.0.0.1:"+port+"/statistics", StatisticsDto.class);

        assertThat(response.getBody().getCount(), is(20L));
        assertTrue(response.getBody().getMax().scale() == 2);
        assertTrue(response.getBody().getMin().scale() == 2);
        assertTrue(response.getBody().getAvg().scale() == 2);
        assertTrue(response.getBody().getSum().scale() == 2);

        //Clear]
        ResponseEntity responseDelete = restTemplate.exchange("http://127.0.0.1:"+port+"/transactions", HttpMethod.DELETE,null, ResponseEntity.class);
        assertThat(responseDelete.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
        assertThat(transactionService.getTransactionSize(), is(0L));
    }

    @Test
    public void statistics_after_60secs_of_no_tnx_return_zeros() throws InterruptedException {
        IntStream.rangeClosed(1,3).forEach(count->{
            LocalDateTime timeStamp = LocalDateTime.now().minusSeconds(60 - count);
            TransactionDto dto = new TransactionDto(new BigDecimal(100.29+ count), timeStamp);
            HttpEntity<TransactionDto> request = new HttpEntity<>(dto);
            ResponseEntity response = restTemplate.postForEntity("http://127.0.0.1:"+port+"/transactions", request, ResponseEntity.class);

            assertThat(response.getStatusCode(), equalTo(HttpStatus.CREATED));
        });

        assertThat(transactionService.getTransactionSize(), is(3L));


        //All should expire after 40secs
        log.info("sleeping for 4sec");
        Thread.sleep(4000);

        ResponseEntity<StatisticsDto> response = restTemplate.getForEntity("http://127.0.0.1:"+port+"/statistics", StatisticsDto.class);

        assertThat(response.getBody().getCount(), is(0L));
        assertThat(response.getBody().getMax().doubleValue(), is(0.00));
        assertThat(response.getBody().getMin().doubleValue(), is(0.00));
        assertThat(response.getBody().getAvg().doubleValue(), is(0.00));
        assertThat(response.getBody().getSum().doubleValue(), is(0.00));
        assertTrue(response.getBody().getMax().scale() == 2);
        assertTrue(response.getBody().getMin().scale() == 2);
        assertTrue(response.getBody().getAvg().scale() == 2);
        assertTrue(response.getBody().getSum().scale() == 2);

        //Clear]
        ResponseEntity responseDelete = restTemplate.exchange("http://127.0.0.1:"+port+"/transactions", HttpMethod.DELETE,null, ResponseEntity.class);
        assertThat(responseDelete.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
        assertThat(transactionService.getTransactionSize(), is(0L));
    }

    @Test
    public void statistics_with_empty_store_return_zeros(){
        ResponseEntity<StatisticsDto> response = restTemplate.getForEntity("http://127.0.0.1:"+port+"/statistics", StatisticsDto.class);

        assertThat(response.getBody().getCount(), is(0L));
        assertThat(response.getBody().getMax().doubleValue(), is(0.00));
        assertThat(response.getBody().getMin().doubleValue(), is(0.00));
        assertThat(response.getBody().getAvg().doubleValue(), is(0.00));
        assertThat(response.getBody().getSum().doubleValue(), is(0.00));
        assertTrue(response.getBody().getMax().scale() == 2);
        assertTrue(response.getBody().getMin().scale() == 2);
        assertTrue(response.getBody().getAvg().scale() == 2);
        assertTrue(response.getBody().getSum().scale() == 2);

        //Clear]
        ResponseEntity responseDelete = restTemplate.exchange("http://127.0.0.1:"+port+"/transactions", HttpMethod.DELETE,null, ResponseEntity.class);
        assertThat(responseDelete.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
        assertThat(transactionService.getTransactionSize(), is(0L));
    }
}
