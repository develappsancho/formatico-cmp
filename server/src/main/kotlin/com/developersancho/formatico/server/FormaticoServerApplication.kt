package com.developersancho.formatico.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/** Formatico's server: the encrypted shares, and the fetch of a document for the web app. */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(FormaticoProperties::class)
class FormaticoServerApplication

/** Starts the server. */
@Suppress("SpreadOperator") // Spring takes the arguments as an array, once, at start.
fun main(args: Array<String>) {
    runApplication<FormaticoServerApplication>(*args)
}
