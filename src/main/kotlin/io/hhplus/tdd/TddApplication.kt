package io.hhplus.tdd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication

// (exclude = [DataSourceAutoConfiguration::class]) : DB 없이 서버 띄우기
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
class TddApplication

fun main(args: Array<String>) {
    runApplication<TddApplication>(*args)
}
