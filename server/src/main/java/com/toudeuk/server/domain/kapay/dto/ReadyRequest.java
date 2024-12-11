package com.toudeuk.server.domain.kapay.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ReadyRequest {
	private String cid;
	private String partnerOrderId;
	private String partnerUserId;
	private String itemName;
	private Integer quantity;
	private Integer totalAmount;
	private Integer taxFreeAmount;
	private Integer vatAmount;
	private String approvalUrl;
	private String cancelUrl;
	private String failUrl;
}
