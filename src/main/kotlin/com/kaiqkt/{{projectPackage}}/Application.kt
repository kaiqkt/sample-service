package com.kaiqkt.`{{projectPackage}}`

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.kaiqkt.`{{projectPackage}}`", "commons.spring"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}