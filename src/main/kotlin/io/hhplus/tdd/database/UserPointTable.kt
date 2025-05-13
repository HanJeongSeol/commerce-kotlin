package io.hhplus.tdd.database

import io.hhplus.tdd.point.UserPoint
import org.springframework.stereotype.Component

/**
 * 해당 Table 클래스는 변경하지 않고 공개된 API 만을 사용해 데이터를 제어합니다.
 */
@Component
class UserPointTable {
    private val table = HashMap<Long, UserPoint>()

    fun selectById(id: Long): UserPoint {
        // 1. 지연 시뮬레이션
        // DB 조회 시 네트워크 딜레이 혹은 I/O 시간이 걸리는 것을 따라함.
        // 동시성 테스트 시 스레드 꼬이는 현상 발생
        Thread.sleep(Math.random().toLong() * 200L)
        // table[id] ?: ... (엘비스 연산자)
        // table에서 id로 값을 꺼냈을 떄 null 이면 ?: 뒤에있는 새 객체를 반환
        return table[id] ?: UserPoint(id = id, point = 0, updateMillis = System.currentTimeMillis())
    }

    fun insertOrUpdate(id: Long, amount: Long): UserPoint {
        Thread.sleep(Math.random().toLong() * 300L)
        val userPoint = UserPoint(id = id, point = amount, updateMillis = System.currentTimeMillis())
        table[id] = userPoint
        return userPoint
    }
}