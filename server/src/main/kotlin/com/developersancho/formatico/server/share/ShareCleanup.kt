package com.developersancho.formatico.server.share

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

/** Deletes the shares that have expired from [store]: when the server starts, and every hour after. */
class ShareCleanup(private val store: ShareStore) {
    private val logger = LoggerFactory.getLogger(ShareCleanup::class.java)

    /** Deletes them now. */
    @Scheduled(initialDelay = 0, fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun deleteExpired() {
        val deleted = store.deleteExpired()
        if (deleted > 0) logger.info("Deleted {} expired shares", deleted)
    }
}
