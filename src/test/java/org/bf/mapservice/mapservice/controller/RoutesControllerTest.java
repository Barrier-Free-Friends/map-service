//package org.bf.mapservice.mapservice.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.bf.mapservice.mapservice.application.query.FindRouteQuery;
//import org.bf.mapservice.mapservice.application.service.RouteApplicationService;
//import org.bf.mapservice.mapservice.domain.entity.MobilityType;
//import org.bf.mapservice.mapservice.presentation.controller.dto.RouteRequestDto;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//class RoutesControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    // 진짜 RouteApplicationService 대신 Mock을 주입해서 DB, Dijkstra는 안 타게 함
//    @MockitoBean
//    private RouteApplicationService routeApplicationService;
//
//    @Test
//    @DisplayName("컨트롤러: /routes 경로 요청 시 경로 노드 ID 리스트를 반환한다")
//    void findRoute_success() throws Exception {
//        // given
//        RouteRequestDto request = new RouteRequestDto(
//                35.0,          // startLatitude
//                129.0,         // startLongitude
//                35.001,        // endLatitude
//                129.001,       // endLongitude
//                MobilityType.WHEELCHAIR
//        );
//
//        List<Long> pathNodeIds = List.of(1L, 2L, 3L);
//
//        given(routeApplicationService.findRoute(any(FindRouteQuery.class)))
//                .willReturn(pathNodeIds);
//
//        // when & then
//        mockMvc.perform(
//                        post("/routes")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(request))
//                )
//                .andExpect(status().isOk())
//                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.nodeIds[0]").value(1L))
//                .andExpect(jsonPath("$.nodeIds[1]").value(2L))
//                .andExpect(jsonPath("$.nodeIds[2]").value(3L));
//    }
//
//    @Test
//    @DisplayName("컨트롤러: 경로가 없는 경우 빈 배열을 반환한다")
//    void findRoute_noPath_returnsEmptyList() throws Exception {
//        // given
//        RouteRequestDto request = new RouteRequestDto(
//                35.0,
//                129.0,
//                36.0,
//                130.0,
//                MobilityType.WHEELCHAIR
//        );
//
//        given(routeApplicationService.findRoute(any(FindRouteQuery.class)))
//                .willReturn(List.of()); // 경로 없음
//
//        // when & then
//        mockMvc.perform(
//                        post("/routes")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(objectMapper.writeValueAsString(request))
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.nodeIds").isArray())
//                .andExpect(jsonPath("$.nodeIds").isEmpty());
//    }
//
//    @Test
//    @DisplayName("컨트롤러: mobilityType 누락 등 잘못된 요청이면 400을 반환한다")
//    void findRoute_invalidRequest_returnsBadRequest() throws Exception {
//        // given
//        String invalidJson = """
//                {
//                  "startLatitude": 35.0,
//                  "startLongitude": 129.0,
//                  "endLatitude": 35.001,
//                  "endLongitude": 129.001
//                }
//                """;
//
//        // when & then
//        mockMvc.perform(
//                        post("/routes")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(invalidJson)
//                )
//                .andExpect(status().isBadRequest());
//    }
//}
