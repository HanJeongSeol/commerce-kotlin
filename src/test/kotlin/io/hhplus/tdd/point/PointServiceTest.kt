package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PointService 단위 테스트")
class PointServiceTest {
    private lateinit var userPointTable: UserPointTable
    private lateinit var pointHistoryTable: PointHistoryTable
    private lateinit var pointService: PointService

    @BeforeEach
    fun setUp() {
        userPointTable = UserPointTable()
        pointHistoryTable = PointHistoryTable()
        pointService = PointService(userPointTable, pointHistoryTable)
    }

    /**
     * [Custom Command] 유저의 초기 포인트 설정
     */
    private fun givenUserPoint(userId: Long, amount: Long) {
        userPointTable.insertOrUpdate(userId, amount)
    }

    /**
     * [Custom Command] 특정 로직 실패 시 IllegalArgumentException과 예외 메시지 검증
     * 고차함수 : 함수를 파라미터로 받음
     * `block: () -> Unit` : 파라미터로 함수(코드 덩어리)를 받는다는 의미
     * () : 인자를 받지 않음
     * Unit : 이 함수는 반환값이 없음
     * `{ block() }` : 넘겨받은 함수 block를 실행하는 코드
     */
    private fun assertError(message: String, block: () -> Unit) {
        assertThatThrownBy { block() } // 1. 전달받은 코드 블록 실행
            .isInstanceOf(IllegalArgumentException::class.java) // 2. 예외 타입 검증
            .hasMessageContaining(message)  // 3. 예외 메시지 검증
    }

    /**
     * [Custom Command] 포인트 내역 객체의 필드 검증
     */
    private fun assertHistory(
        history: PointHistory,
        expectedUserId: Long,
        expectedType: TransactionType,
        expectedAmount: Long
    ) {
        assertThat(history.userId).isEqualTo(expectedUserId)
        assertThat(history.type).isEqualTo(expectedType)
        assertThat(history.amount).isEqualTo(expectedAmount)
    }

    @Nested
    @DisplayName("포인트 조회 기능")
    // 코틀린의 중첩 클래스는 기본적으로 static이기 떄문에 바깥 클래스(PointServiceTest)의 멤버에 접근하려면 inner를 붙여야 한다.
    inner class GetPointTest {
        @Test
        @DisplayName("포인트 조회 - 성공")
        fun `사용자 ID로 포인트 조회 시 보유한 포인트 반환`() {
            // given
            val userId = 1L
            val expectedPoint = 1000L
            givenUserPoint(userId, expectedPoint)

            // when
            val result = pointService.getPoint(userId)

            // then
            assertThat(result.id).isEqualTo(userId)
            assertThat(result.point).isEqualTo(expectedPoint)
        }
    }

    @Nested
    @DisplayName("포인트 충전 기능")
    inner class ChargePointTest {
        @Test
        @DisplayName("포인트 충전 - 성공")
        fun `5000 포인트 충전 시 6000 포인트 반환에 성공한다`() {
            // given
            val userId = 1L
            val initialPoint = 1000L
            val chargeAmount = 5000L
            givenUserPoint(userId, initialPoint)

            // when
            val result = pointService.chargePoint(userId, chargeAmount)

            // then
            assertThat(result.id).isEqualTo(userId)
            assertThat(result.point).isEqualTo(initialPoint + chargeAmount)

        }

        @Test
        @DisplayName("포인트 충전 - 충전 후 정확히 10,000,000원일 때 성공")
        fun `충전 후 잔액이 정확히 10,000,000원 일 때 성공`() {
            // given
            val userId = 1L
            val initialPoint = 5_000_000L
            val chargeAmount = 5_000_000L
            givenUserPoint(userId, initialPoint)

            // when
            val result = pointService.chargePoint(userId, chargeAmount)

            // then
            assertThat(result.point).isEqualTo(10_000_000L)
        }

        @Test
        @DisplayName("포인트 충전 - 충전 금액이 1000미만일 때 예외 발생")
        fun `충전 금액이 1000 이하일 때 예외 발생`() {
            // given
            val userId = 1L
            val invalidAmount = 500L

            // when & then
            assertError("1000원 미만은 충전할 수 없습니다.") {
                pointService.chargePoint(userId, invalidAmount)
            }
        }

        @Test
        @DisplayName("포인트 충전 - 충전 후 보유 금액이 10,000,000를 초과하는 경우 예외 발생")
        fun `충전 후 잔액이 10,000,000 초과일 때 예외 발생`() {

            // given
            val userId = 1L
            val initialPoint = 9_000_000L
            val chargeAmount = 1_000_001L
            givenUserPoint(userId, initialPoint)

            // when & then
            assertError("충전 후 포인트가 10,000,000를 초과할 수 없습니다.") {
                pointService.chargePoint(userId, chargeAmount)
            }
        }

        @Test
        @DisplayName("포인트 충전 - 음수 금액 충전 시 예외 발생")
        fun `음수 금액을 충전할 시 예외 발생`() {
            // given
            val userId = 1L
            val negativeAmount = -1000L

            // when & then
            assertError("1000원 미만은 충전할 수 없습니다.") {
                pointService.chargePoint(userId, negativeAmount)
            }
        }
    }


    @Nested
    @DisplayName("포인트 사용 기능")
    inner class UsePointTest {
        @Test
        @DisplayName("포인트 사용 - 성공")
        fun `1000 포인트 사용에 성공한다`() {
            // given
            val userId = 1L
            val initialPoint = 5000L
            val usePoint = 1000L
            givenUserPoint(userId, initialPoint)

            // when
            val result = pointService.usePoint(userId, usePoint)

            // then
            assertThat(result.id).isEqualTo(userId)
            assertThat(result.point).isEqualTo(initialPoint - usePoint)
        }

        @Test
        @DisplayName("포인트 사용 후 0원이 되는 경우 성공")
        fun `잔액을 모두 사용하여 0원이 되는 경우 성공`() {
            // given
            val userId = 1L
            val initialPoint = 5000L
            val usePoint = 5000L
            givenUserPoint(userId, initialPoint)

            // when
            val result = pointService.usePoint(userId, usePoint)

            // then
            assertThat(result.point).isEqualTo(0L)

        }

        @Test
        @DisplayName("포인트 사용 - 포인트 사용 금액이 0이하일 때 예외 발생")
        fun `포인트 사용 금액이 0이하일 때 예외 발생`() {
            // given
            val userId = 1L
            val initialPoint = 1_000_000L
            val userAmount = 0L
            givenUserPoint(userId, initialPoint)

            // when & then
            assertError("0포인트 미만은 사용할 수 없습니다.") {
                pointService.usePoint(userId, userAmount)
            }
        }

        @Test
        @DisplayName("포인트 사용 - 포인트 사용 후 남은 잔액이 0미만일 때 예외 발생")
        fun `포인트 사용 후 잔액이 0미만인 경우 예외 발생`() {
            // given
            val userId = 1L
            val initialPoint = 5_000L
            val useAmount = 6_000L
            givenUserPoint(userId, initialPoint)

            // when & then
            assertError("보유 포인트가 부족합니다.") {
                pointService.usePoint(userId, useAmount)
            }
        }

        @Test
        @DisplayName("포인트 사용 - 음수 금액 사용 시 예외 발생")
        fun `음수 금액 사용 시 예외 발생`() {
            // given
            val userId = 1L
            val initialPoint = 5_000_000L
            val negativeAmount = -1000L
            givenUserPoint(userId, initialPoint)

            // when & then
            assertError("0포인트 미만은 사용할 수 없습니다.") {
                pointService.usePoint(userId, negativeAmount)
            }
        }
    }

    @Nested
    @DisplayName("포인트 내역 조회 기능")
    inner class GetHistoryTest {
        @Test
        @DisplayName("포인트 내역 조회 - 포인트 내역 조회 성공")
        fun `포인트 내역 조회 시 사용자 아이디, 잔액, 타입, 수정 시간 조회에 성공`() {
            // given
            val userId = 1L
            val initialPoint = 10_000L
            givenUserPoint(userId, initialPoint)

            // 조회를 위한 충전 및 사용 실행
            pointService.chargePoint(userId, 5_000L)
            pointService.usePoint(userId, 3_000L)

            // when
            val histories = pointService.getHistories(userId)

            // then
            assertThat(histories).hasSize(2)    // 충전 및 사용 2개의 내역

            // Custom Command로 내역 상세 검증
            assertHistory(histories[0], userId, TransactionType.CHARGE, 5_000L)
            assertHistory(histories[1], userId, TransactionType.USE, 3_000L)

        }

        @Test
        @DisplayName("포인트 내역 조회 - 내역이 없을 떄 빈 리스트 반환")
        fun `포인트 내역이 없는 유저 조회 시 빈 리스트 반환`() {
            // given
            val userId = 999L

            // when
            val histories = pointService.getHistories(userId)

            // then
            assertThat(histories).isEmpty()
        }

        @Test
        @DisplayName("포인트 내역 조회 - 다른 유저의 내역은 조회되지 않음")
        fun `특정 유저의 내역만 조회되고 다른 유저 내역은 포함하지 않음`() {
            // given
            val userId1 = 1L
            val userId2 = 2L
            val initialPoint = 10_000L
            givenUserPoint(userId1, initialPoint)
            givenUserPoint(userId2, initialPoint)

            // 각 유저가 충전
            pointService.chargePoint(userId1, 5_000L)
            pointService.chargePoint(userId2, 3_000L)
            pointService.chargePoint(userId1, 1_000L)

            // when
            val histories = pointService.getHistories(userId1)

            // then
            assertThat(histories).hasSize(2)
            // allMatch - 리스트의 모든 요소가 조건을 만족하는지 확인,
            // it - 람다식 단일 파라미터 기본 이름
            assertThat(histories).allMatch { it.userId == userId1 }
        }
    }
}