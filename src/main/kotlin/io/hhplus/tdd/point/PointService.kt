package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.springframework.stereotype.Service

@Service
class PointService (
    private val userPointTable: UserPointTable,
    private val pointHistoryTable: PointHistoryTable,
) {
    fun getPoint(userId: Long): UserPoint {
        return userPointTable.selectById(userId)
    }

    fun chargePoint(userId:Long, chargeAmount:Long): UserPoint{
        val userPoint = userPointTable.selectById(userId)
        if (chargeAmount < 1000L){
            throw IllegalArgumentException("1000원 미만은 충전할 수 없습니다.")
        }
        val amount = userPoint.point+chargeAmount
        if (amount > 10_000_000L){
            throw IllegalArgumentException("충전 후 포인트가 10,000,000를 초과할 수 없습니다.")
        }
        pointHistoryTable.insert(userId,chargeAmount, TransactionType.CHARGE,System.currentTimeMillis())
        return userPointTable.insertOrUpdate(userId, amount)
    }

    fun usePoint(userId: Long, useAmount:Long): UserPoint {
        val userPoint = userPointTable.selectById(userId)
        if(useAmount <= 0){
            throw IllegalArgumentException("0포인트 미만은 사용할 수 없습니다.")
        }
        val amount = userPoint.point - useAmount
        if (amount < 0){
            throw IllegalArgumentException("보유 포인트가 부족합니다.")
        }
        pointHistoryTable.insert(userId, useAmount, TransactionType.USE, System.currentTimeMillis())
        return userPointTable.insertOrUpdate(userId, amount)
    }

    fun getHistories(userId:Long) : List<PointHistory> = pointHistoryTable.selectAllByUserId(userId)
}