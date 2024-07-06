package com.jonggae.yakku.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor

public class EventDto {
    private String eventType;
    private Long orderId;
    private Long productId;
    private Long customerId;
    private Map<String, Object> data;

    @JsonCreator
    public EventDto(
            @JsonProperty("eventType") String eventType,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("productId") Long productId,
            @JsonProperty("customerId") Long customerId,
            @JsonProperty("data") Map<String, Object> data) {
        this.eventType = eventType;
        this.orderId = orderId;
        this.productId = productId;
        this.customerId = customerId;
        this.data = data;
    }
}
