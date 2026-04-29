// service_spring/src/main/java/com/bayesian/dto/response/AssimilationResponse.java

package com.bayesian.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AssimilationResponse {
    private String jobId;
    private String status;
    private List<List<List<Double>>> analysisField;
    private List<List<List<Double>>> varianceField;
    private double computationTime;
    private LocalDateTime timestamp;
    private String message;
}