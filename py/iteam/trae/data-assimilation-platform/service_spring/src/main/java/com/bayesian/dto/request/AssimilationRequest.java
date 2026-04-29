// service_spring/src/main/java/com/bayesian/dto/request/AssimilationRequest.java

package com.bayesian.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AssimilationRequest {
    private String jobId;
    private List<List<List<Double>>> backgroundField;
    private List<Double> observations;
    private List<List<Double>> obsLocations;
    private Map<String, Object> config;
    private boolean allowDegraded;
}