package org.bf.mapservice.mapservice.presentation.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteDetailResponseDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.RouteRequestDto;

@Tag(name = "Routes", description = "길찾기 API")
public interface RoutesApiDoc {

    @Operation(
            summary = "길찾기",
            description = "일반 사용자/배리어프리 사용자 기준으로 GraphHopper 라우팅을 수행합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "경로 조회 성공",
                    content = @Content(schema = @Schema(implementation = RouteDetailResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패"),
            @ApiResponse(responseCode = "404", description = "경로를 찾지 못함(ROUTE_NOT_FOUND)"),
            @ApiResponse(responseCode = "404", description = "이동수단에 맞는 경로 없음(ROUTE_NOT_SUITABLE_MOBILITY)")
    })
    RouteDetailResponseDto findRouteDetail(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "출발/도착 좌표와 이동수단",
                    content = @Content(
                            schema = @Schema(implementation = RouteRequestDto.class),
                            examples = @ExampleObject(value = """
                            {
                              "startLatitude": 37.501,
                              "startLongitude": 127.001,
                              "endLatitude": 37.510,
                              "endLongitude": 127.010,
                              "mobilityType": "WHEELCHAIR"
                            }
                            """)
                    )
            )
            RouteRequestDto request
    );
}
