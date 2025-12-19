package org.bf.mapservice.mapservice.presentation.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bf.mapservice.mapservice.presentation.controller.dto.CreateObstacleRequestDto;
import org.bf.mapservice.mapservice.presentation.controller.dto.ObstacleFeatureCollectionDto;

@Tag(name = "Obstacles", description = "장애물 API")
public interface ObstaclesApiDoc {

    @Operation(summary = "장애물 생성", description = "POINT 또는 LINESTRING 기반으로 장애물을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공(장애물 ID 반환)"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패")
    })
    Long create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateObstacleRequestDto.class),
                            examples = {
                                    @ExampleObject(name = "POINT 예시", value = """
                                    {
                                      "geomType": "POINT",
                                      "point": [127.0055, 37.5055],
                                      "type": "ROCK",
                                      "severity": "CRITICAL",
                                      "radiusMeters": 30
                                    }
                                    """),
                                    @ExampleObject(name = "LINESTRING 예시", value = """
                                    {
                                      "geomType": "LINESTRING",
                                      "line": [[127.001, 37.501], [127.01, 37.51]],
                                      "type": "CONSTRUCTION",
                                      "severity": "HIGH",
                                      "radiusMeters": 40
                                    }
                                    """)
                            }
                    )
            )
            CreateObstacleRequestDto req
    );

    @Operation(summary = "장애물 해결(삭제 처리)", description = "id에 해당하는 장애물을 RESOLVED 처리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "처리 성공"),
            @ApiResponse(responseCode = "404", description = "장애물 없음(OBSTACLE_NOT_FOUND)")
    })
    void resolve(
            @Parameter(description = "장애물 ID", required = true, in = ParameterIn.PATH)
            Long id
    );

    @Operation(summary = "활성 장애물 조회", description = "Envelope(minLon,minLat,maxLon,maxLat) 안 ACTIVE 장애물 GeoJSON 반환")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ObstacleFeatureCollectionDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "파라미터 누락/형식 오류")
    })
    ObstacleFeatureCollectionDto getActiveObstacles(
            @Parameter(description = "최소 경도", example = "127.000") Double minLon,
            @Parameter(description = "최소 위도", example = "37.500") Double minLat,
            @Parameter(description = "최대 경도", example = "127.011") Double maxLon,
            @Parameter(description = "최대 위도", example = "37.511") Double maxLat
    );
}
