package org.bf.mapservice.mapservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bf.mapservice.mapservice.application.query.FindRouteQuery;
import org.bf.mapservice.mapservice.application.service.RouteApplicationService;
import org.bf.mapservice.mapservice.domain.entity.MobilityType;
import org.bf.mapservice.mapservice.presentation.controller.dto.RoutePointDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoutesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 진짜 RouteApplicationService 대신 Mock을 주입해서 DB, pgRouting은 안 타게 함
    @MockitoBean
    private RouteApplicationService routeApplicationService;

    @Test
    @DisplayName("POST /routes - 정상 요청 시 경로 좌표 리스트를 반환한다")
    void findRoute_success() throws Exception {
        // given
        RouteRequestDto request = new RouteRequestDto(
                37.4890633,      // startLatitude
                127.0338167,     // startLongitude
                37.4889748,      // endLatitude
                127.0336716,     // endLongitude
                MobilityType.WHEELCHAIR
        );

        List<RoutePointDto> points = List.of(
                new RoutePointDto(37.4890633, 127.0338167),
                new RoutePointDto(37.4889748, 127.0336716)
        );

        given(routeApplicationService.findRoute(any(FindRouteQuery.class)))
                .willReturn(points);

        // when & then
        mockMvc.perform(
                        post("/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // JSON 구조: { "points": [ { "latitude": ..., "longitude": ... }, ... ] }
                .andExpect(jsonPath("$.points[0].latitude").value(points.get(0).latitude()))
                .andExpect(jsonPath("$.points[0].longitude").value(points.get(0).longitude()))
                .andExpect(jsonPath("$.points[1].latitude").value(points.get(1).latitude()))
                .andExpect(jsonPath("$.points[1].longitude").value(points.get(1).longitude()));
    }

    @Test
    @DisplayName("POST /routes - 경로가 없는 경우 points는 빈 배열을 반환한다")
    void findRoute_noPath_returnsEmptyPoints() throws Exception {
        // given
        RouteRequestDto request = new RouteRequestDto(
                37.50,
                127.00,
                37.60,
                127.10,
                MobilityType.WHEELCHAIR
        );

        given(routeApplicationService.findRoute(any(FindRouteQuery.class)))
                .willReturn(List.of()); // 경로 없음

        // when & then
        mockMvc.perform(
                        post("/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").isEmpty());
    }

    @Test
    @DisplayName("POST /routes - mobilityType 누락 등 잘못된 요청이면 400을 반환한다")
    void findRoute_invalidRequest_returnsBadRequest() throws Exception {
        // given: mobilityType 누락된 JSON
        String invalidJson = """
                {
                  "startLatitude": 37.4890633,
                  "startLongitude": 127.0338167,
                  "endLatitude": 37.4889748,
                  "endLongitude": 127.0336716
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/routes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                )
                .andExpect(status().isBadRequest());
    }
}
