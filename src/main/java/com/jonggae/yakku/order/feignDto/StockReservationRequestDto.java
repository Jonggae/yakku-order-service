package com.jonggae.yakku.order.feignDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class StockReservationRequestDto {
    private Long quantity;
}