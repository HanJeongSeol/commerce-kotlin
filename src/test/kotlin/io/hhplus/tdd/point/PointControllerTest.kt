package io.hhplus.tdd.point

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest // 전체 스프링 컨텍스트 로드
@AutoConfigureMockMvc   // MockMvc 자동 설정
@DisplayName("PointController 통합 테스트")
class PointControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userPointTable: UserPointTable

    @Autowired
    private lateinit var pointHistoryTable: PointHistoryTable


    // Custom Command : 유저 초기 포인트 설정
    private fun givenUserPoint(userId: Long, amount: Long) {
        userPointTable.insertOrUpdate(userId, amount)
    }

    // Custom Command : 히스토리 데이터 생성
    private fun givenHistory(userId: Long, amount: Long, type: TransactionType) {
        pointHistoryTable.insert(userId, amount, type, System.currentTimeMillis())
    }




    @Nested
    @DisplayName("GET /api/v1/point/{id}")
    inner class GetPointApiTest{
        @Test
        fun `5000포인트 보유 유저 조회시 5000포인트를 반환한다`() {
            // given
            val userId = 1L
            val expectedPoint = 5_000L
            givenUserPoint(userId, expectedPoint)

            // when & then
            // MockMvc 사용
            // get() : GET 요청
            // "/api/v1/point/{id}", userId : URL 경로 변수 바인딩
            mockMvc.perform(
                get("/api/v1/point/{id}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                // JSON 응답 검증
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedPoint))
        }
    }
    @Nested
    @DisplayName("PATCH /api/v1/point/{id}/charge")
    inner class ChargePointApiTest {

        @Test
        fun `5000포인트 보유 유저가 3000포인트 충전시 8000포인트를 반환한다`() {
            // given
            val userId = 2L
            val initialPoint = 5_000L
            val chargeAmount = 3_000L
            givenUserPoint(userId, initialPoint)

            // when & then
            mockMvc.perform(
                patch("/api/v1/point/{id}/charge", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(chargeAmount))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(8_000L))
        }

        @Test
        fun `500포인트 충전시 InternalServerError를 발생한다`() {
            // given
            val userId = 3L
            val invalidAmount = 500L

            // when & then
            mockMvc.perform(
                patch("/api/v1/point/{id}/charge", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidAmount))
            )
                .andExpect(status().isInternalServerError)
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/point/{id}/use")
    inner class UsePointApiTest {

        @Test
        fun `5000포인트 보유 유저가 3000포인트 사용시 2000포인트를 반환한다`() {
            // given
            val userId = 4L
            val initialPoint = 5_000L
            val useAmount = 3_000L
            givenUserPoint(userId, initialPoint)

            // when & then
            mockMvc.perform(
                patch("/api/v1/point/{id}/use", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(useAmount))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(2_000L))
        }

        @Test
        fun `1000포인트 보유 유저가 4000포인트 사용시 InternalServerError를 발생한다`() {
            // given
            val userId = 5L
            val initialPoint = 1_000L
            val useAmount = 4_000L
            givenUserPoint(userId, initialPoint)

            // when & then
            mockMvc.perform(
                patch("/api/v1/point/{id}/use", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(useAmount))
            )
                .andExpect(status().isInternalServerError)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/point/{id}/histories")
    inner class GetHistoriesApiTest {

        @Test
        fun `5000포인트 충전 및 3000포인트 사용한 유저의 내역 조회시 2건의 내역을 반환한다`() {
            // given
            val userId = 6L
            val initialPoint = 10_000L
            givenUserPoint(userId, initialPoint)

            // 충전 및 사용 실행
            givenHistory(userId, 5_000L, TransactionType.CHARGE)
            givenHistory(userId, 3_000L, TransactionType.USE)

            // when & then
            mockMvc.perform(
                get("/api/v1/point/{id}/histories", userId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].amount").value(5_000L))
                .andExpect(jsonPath("$[1].userId").value(userId))
                .andExpect(jsonPath("$[1].type").value("USE"))
                .andExpect(jsonPath("$[1].amount").value(3_000L))
        }
    }
}