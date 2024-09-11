package com.interaso.webpush

import com.microsoft.playwright.*
import com.microsoft.playwright.assertions.*
import com.microsoft.playwright.assertions.PlaywrightAssertions.*
import dev.whyoleg.cryptography.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.*
import kotlinx.html.*
import org.junit.jupiter.api.*
import java.nio.file.*
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
class BrowserTest {
    @Test
    fun shouldReceiveNotification() {
        runBlocking {
            val webPush = WebPush(
                subject = "mailto:github@morki.cz",
                VapidKeys.fromBase64(
                    publicKey = "BJwwFRoDoOx2vQPfvbeo-m1fZZHo6lIjtyTlWHjLNSCtHuWdGryZD5xt0LeawVQq7G60ioID1sC33fEoQT8jCzg",
                    privateKey = "P5GjTLppISlmUyNiZqZi0HNq7GXFniAdcBECNsKBxfI",
                )
            )

            val data = "Test"

            val server = embeddedServer(CIO, port = 0) {
                routing {
                    staticResources("/", null)

                    get("/") {
                        call.respondHtml {
                            head { script(type = "module", src = "test.js") {} }
                            body {}
                        }
                    }

                    get("/vapid") {
                        call.respondBytes(webPush.getApplicationServerKey())
                    }

                    post("/send") {
                        val params = call.receiveParameters()
                        val endpoint: String by params
                        val p256dh: String by params
                        val auth: String by params

                        val notification = Notification(data)
                        val subscription = Subscription(endpoint, p256dh, auth)

                        webPush.send(subscription, notification)
                        call.respondText("OK")
                    }
                }
            }

            startServer(server) { port ->
                openPage("http://127.0.0.1:$port") { page ->
                    assertThat(page.locator("body")).hasText(
                        data,
                        LocatorAssertions.HasTextOptions().setTimeout(60_000.0),
                    )
                }
            }
        }
    }

    private fun openPage(url: String, block: (Page) -> Unit) {
        val tempDir = Files.createTempDirectory("test")

        val contextOptions = BrowserType.LaunchPersistentContextOptions()
            .setHeadless(false) // TODO: does not work in headless mode
            .setChromiumSandbox(false)
            .setIgnoreHTTPSErrors(true)
            .setPermissions(listOf("notifications"))

        try {
            Playwright.create().use { playwright ->
                playwright.chromium().launchPersistentContext(tempDir, contextOptions).use { context ->
                    context.newPage().use { page ->
                        page.navigate(url)
                        block(page)
                    }
                }
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private suspend fun startServer(server: ApplicationEngine, block: (Int) -> Unit) {
        try {
            server.start()
            block(server.resolvedConnectors().first().port)
        } finally {
            server.stop()
        }
    }
}
