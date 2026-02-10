package com.brightflare.claudeping

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ClaudePingApplication

fun main(args: Array<String>) {
    runApplication<ClaudePingApplication>(*args)
}
