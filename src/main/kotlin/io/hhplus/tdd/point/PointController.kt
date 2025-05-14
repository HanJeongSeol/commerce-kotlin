package io.hhplus.tdd.point

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/point")
class PointController (private val pointService: PointService){
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * [id]를 기반으로 사용자의 포인트 내역을 조회.
     *
     * @return 포인트를 포함한 [UserPoint] 정보
     */
    @GetMapping("{id}")
    fun point(
        @PathVariable id: Long,
    ): UserPoint {
        logger.info("포인트 조회 요청 - userId: $id")
        return pointService.getPoint(id)
    }

    /**
     * 특정 유저의 포인트 충전/사용 내역을 조회하는 기능
     *
     * @param id    사용자 ID
     * @return [PointHistory]
     */
    @GetMapping("{id}/histories")
    fun history(
        @PathVariable id: Long,
    ): List<PointHistory> {
        logger.info("포인트 내역 조회 요청 - userId: $id")
        return pointService.getHistories(id)
    }


    /**
     * 특정 유저의 포인트를 충전하는 기능
     *
     * @param id        사용자 ID
     * @param amount    포인트
     * @return          [UserPoint]
     */
    @PatchMapping("{id}/charge")
    fun charge(
        @PathVariable id: Long,
        @RequestBody amount: Long,
    ): UserPoint {
        logger.info("포인트 충전 요청 - userId: $id, amount: $amount")
        return pointService.chargePoint(id,amount)
    }

    /**
     * 특정 유저의 포인트를 사용하는 기능
     *
     * @param id        사용자 ID
     * @param amount    포인트
     * @return          [UserPoint]
     */
    @PatchMapping("{id}/use")
    fun use(
        @PathVariable id: Long,
        @RequestBody amount: Long,
    ): UserPoint {
        logger.info("포인트 사용 요청 - userId: $id, amount: $amount")
        return pointService.usePoint(id,amount)
    }
}