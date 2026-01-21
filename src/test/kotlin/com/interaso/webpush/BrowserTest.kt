package com.interaso.webpush

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.assertions.LocatorAssertions
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.util.getValue
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlinx.coroutines.runBlocking
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.script
import org.junit.jupiter.api.Test

@OptIn(ExperimentalPathApi::class)
class BrowserTest {
    @Test
    fun shouldReceiveNotification() {
        val vapidKeys = VapidKeys.fromUncompressedBytes(
            "BJwwFRoDoOx2vQPfvbeo-m1fZZHo6lIjtyTlWHjLNSCtHuWdGryZD5xt0LeawVQq7G60ioID1sC33fEoQT8jCzg",
            "P5GjTLppISlmUyNiZqZi0HNq7GXFniAdcBECNsKBxfI",
        )

        val webPush = WebPushService("mailto:oss@interaso.com", vapidKeys)
        val notification = "Test"

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
                    call.respondBytes(webPush.vapidKeys.applicationServerKey)
                }

                post("/send") {
                    val params = call.receiveParameters()
                    val endpoint: String by params
                    val p256dh: String by params
                    val auth: String by params

                    webPush.send(notification, endpoint, p256dh, auth)
                    call.respondText("OK")
                }
            }
        }

        startServer(server) { port ->
            openPage("http://127.0.0.1:$port") { page ->
                assertThat(page.locator("body")).hasText(
                    notification,
                    LocatorAssertions.HasTextOptions().setTimeout(60_000.0),
                )
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

    private fun startServer(server: EmbeddedServer<*, *>, block: (Int) -> Unit) {
        try {
            server.start()
            block(runBlocking { server.engine.resolvedConnectors().first().port })
        } finally {
            server.stop()
        }
    }
}
