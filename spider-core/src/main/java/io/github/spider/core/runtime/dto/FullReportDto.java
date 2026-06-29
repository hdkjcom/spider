package io.github.spider.core.runtime.dto;

import java.util.List;
import java.util.Map;

/**
 * Full report DTO containing summary, recent errors, client stats, and circuit breaker states.
 */
public class FullReportDto {

    private RuntimeSummaryDto summary;
    private List<ErrorEntryDto> recentErrors;
    private Map<String, ClientStatsDto> clients;
    private Map<String, String> circuitBreakers;

    public FullReportDto() {
    }

    public FullReportDto(RuntimeSummaryDto summary, List<ErrorEntryDto> recentErrors,
                         Map<String, ClientStatsDto> clients, Map<String, String> circuitBreakers) {
        this.summary = summary;
        this.recentErrors = recentErrors;
        this.clients = clients;
        this.circuitBreakers = circuitBreakers;
    }

    public RuntimeSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(RuntimeSummaryDto summary) {
        this.summary = summary;
    }

    public List<ErrorEntryDto> getRecentErrors() {
        return recentErrors;
    }

    public void setRecentErrors(List<ErrorEntryDto> recentErrors) {
        this.recentErrors = recentErrors;
    }

    public Map<String, ClientStatsDto> getClients() {
        return clients;
    }

    public void setClients(Map<String, ClientStatsDto> clients) {
        this.clients = clients;
    }

    public Map<String, String> getCircuitBreakers() {
        return circuitBreakers;
    }

    public void setCircuitBreakers(Map<String, String> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }
}
