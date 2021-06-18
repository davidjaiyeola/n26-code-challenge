package com.n26.controllers;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.n26.dto.StatisticsDto;
import com.n26.dto.TransactionDto;
import com.n26.exception.FutureDateValidationException;
import com.n26.exception.NotWithInRangeValidationException;
import com.n26.service.TransactionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeParseException;

@RestController
@Api(value = "Statistics", description = "Rest API for Statistics operations", tags = "Statistics API")
public class TransactionController {
    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity handleException(Throwable ex) {
        log.error(ex.getMessage());
        if (ex instanceof HttpMessageNotReadableException) {
            Throwable actualCause = ex.getCause();
            if(actualCause instanceof InvalidFormatException) {
                return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
            }else if(actualCause instanceof MismatchedInputException) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }else if (ex instanceof FutureDateValidationException) {
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }else if (ex instanceof NotWithInRangeValidationException) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(null,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ApiOperation(value = "Create Transaction", response = ResponseEntity.class, consumes = "application/json", produces = "application/json")
    @PostMapping(value = "/transactions", consumes = "application/json")
    public ResponseEntity addTransaction(@RequestBody TransactionDto transaction) throws FutureDateValidationException,NotWithInRangeValidationException{

        transactionService.addTransaction(transaction);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @ApiOperation(value = "Clear Transactions")
    @DeleteMapping(value = "/transactions")
    public ResponseEntity deleteAllTransactions() {
        if(transactionService.deleteAllTransactions()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }else{
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/statistics")
    @ApiOperation(value = "Get Statistics", response = StatisticsDto.class, produces = "application/json")
    public StatisticsDto statistics() {
        return transactionService.getStatistics();
    }
}
