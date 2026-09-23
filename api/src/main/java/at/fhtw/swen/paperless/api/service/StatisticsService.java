package at.fhtw.swen.paperless.api.service;

import at.fhtw.swen.paperless.api.controller.response.TagStatisticsResponse;

import java.util.List;

public interface StatisticsService {

    List<TagStatisticsResponse> getTagStatistics();

}
