package com.n26.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsDto {
    private BigDecimal sum;
    private BigDecimal avg;
    private BigDecimal max;
    private BigDecimal min;
    private long count;
}
