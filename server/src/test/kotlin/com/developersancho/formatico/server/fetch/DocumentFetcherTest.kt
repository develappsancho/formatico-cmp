package com.developersancho.formatico.server.fetch

import com.developersancho.formatico.core.share.ShareServer
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.springframework.http.MediaType
import java.net.InetAddress
import java.net.ServerSocket
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** The fetch against a server on this machine: what it reads, what it refuses, and how it fails. */
class DocumentFetcherTest {
    private val server = MockWebServer()
    private val loopback = InetAddress.getByName("127.0.0.1")
    private val fetchers = mutableListOf<DocumentFetcher>()

    /** The public guard, on every port, which takes 127.0.0.1 alone besides: the mock server, and nothing else of the machine's. */
    private val mockOnly = FetchGuard({ it == loopback || PublicAddresses.isPublic(it) }, { true })

    @BeforeTest
    fun start() {
        server.start(loopback, 0)
    }

    @AfterTest
    fun stop() {
        server.close()
        fetchers.forEach(DocumentFetcher::close)
    }

    @Test
    fun readsTheDocumentAndItsContentType() = runBlocking {
        server.enqueue(response(200, """{"id": 7}""", "Content-Type" to "application/json; charset=utf-8"))
        val document = assertIs<Fetched.Document>(fetcher().fetch(url("/orders/7.json?full=1")))
        assertContentEquals("""{"id": 7}""".encodeToByteArray(), document.bytes)
        assertEquals(MediaType.parseMediaType("application/json;charset=UTF-8"), document.type)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/orders/7.json?full=1", request.target)
    }

    @Test
    fun sendsNothingOfTheUsers() = runBlocking {
        server.enqueue(response(200, "a,b"))
        fetcher().fetch(url("/table.csv"))
        val headers = server.takeRequest().headers
        assertEquals(DocumentFetcher.USER_AGENT, headers["User-Agent"])
        for (name in listOf("Cookie", "Authorization", "Proxy-Authorization", "Referer", "Origin", "X-Forwarded-For")) {
            assertNull(headers[name], name)
        }
    }

    @Test
    fun readsAnEmptyDocument() = runBlocking {
        server.enqueue(response(204, ""))
        val document = assertIs<Fetched.Document>(fetcher().fetch(url("/empty")))
        assertEquals(0, document.bytes.size)
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, document.type)
    }

    @Test
    fun readsTheLargestDocumentAFetchReads() = runBlocking {
        val bytes = ByteArray(ShareServer.MAX_FETCH_BYTES) { (it % 251).toByte() }
        server.enqueue(MockResponse.Builder().code(200).chunkedBody(Buffer().write(bytes), 64 * 1024).build())
        assertContentEquals(bytes, assertIs<Fetched.Document>(fetcher().fetch(url("/large"))).bytes)
    }

    @Test
    fun refusesADocumentItsAddressSaysIsTooLarge() = runBlocking {
        val bytes = ShareServer.MAX_FETCH_BYTES + 1L
        server.enqueue(MockResponse.Builder().code(200).body(Buffer().write(ByteArray(bytes.toInt()))).build())
        assertEquals(Fetched.TooLarge(bytes), fetcher().fetch(url("/huge")))
    }

    @Test
    fun stopsReadingADocumentThatTurnsOutTooLarge() = runBlocking {
        val bytes = ByteArray(ShareServer.MAX_FETCH_BYTES + 1)
        server.enqueue(MockResponse.Builder().code(200).chunkedBody(Buffer().write(bytes), 64 * 1024).build())
        assertEquals(Fetched.TooLarge(null), fetcher().fetch(url("/endless")))
    }

    @Test
    fun answersWithTheStatusOfAnAddressThatGaveNoDocument() = runBlocking {
        for (status in listOf(400, 401, 403, 404, 410, 429, 500, 502, 503, 300, 304)) {
            server.enqueue(response(status, ""))
            assertEquals(Fetched.Status(status), fetcher().fetch(url("/status/$status")), "status $status")
        }
    }

    @Test
    fun followsRedirects() = runBlocking {
        server.enqueue(redirect(301, "/second"))
        server.enqueue(redirect(302, "third"))
        server.enqueue(redirect(303, "http://127.0.0.1:${server.port}/fourth"))
        server.enqueue(redirect(307, "/fifth"))
        server.enqueue(redirect(308, "/sixth"))
        server.enqueue(response(200, "found"))
        val document = assertIs<Fetched.Document>(fetcher().fetch(url("/first")))
        assertContentEquals("found".encodeToByteArray(), document.bytes)
        assertEquals(listOf("/first", "/second", "/third", "/fourth", "/fifth", "/sixth"), List(6) { server.takeRequest().target })
    }

    @Test
    fun followsNoMoreThanFiveRedirects() = runBlocking {
        repeat(DocumentFetcher.MAX_REDIRECTS + 1) { server.enqueue(redirect(302, "/again$it")) }
        assertEquals(Fetched.Status(302), fetcher().fetch(url("/start")))
        assertEquals(DocumentFetcher.MAX_REDIRECTS + 1, server.requestCount)
    }

    @Test
    fun answersARedirectWithNoLocationOrAnUnreadableOneWithItsStatus() = runBlocking {
        server.enqueue(response(302, ""))
        assertEquals(Fetched.Status(302), fetcher().fetch(url("/nowhere")))
        server.enqueue(redirect(301, "http://exa mple.com/"))
        assertEquals(Fetched.Status(301), fetcher().fetch(url("/broken")))
    }

    @Test
    fun refusesARedirectToAnAddressTheFetchMayNotReach() = runBlocking {
        val internal = HostLookup { host ->
            if (host ==
                "internal.test"
            ) {
                listOf(InetAddress.getByName("10.0.0.7"))
            } else {
                throw UnknownHostException(host)
            }
        }
        val fetcher = fetcher(mockOnly, internal)
        val locations = listOf(
            "http://169.254.169.254/latest/meta-data/",
            "http://[::1]:${server.port}/",
            "http://192.168.1.1/admin",
            "http://internal.test:${server.port}/",
            "ftp://127.0.0.1:${server.port}/",
            "https://user:secret@127.0.0.1:${server.port}/"
        )
        for (location in locations) {
            server.enqueue(redirect(302, location))
            assertEquals(Fetched.NotAllowed, fetcher.fetch(url("/to-private")), location)
        }
        assertEquals(locations.size, server.requestCount)
    }

    @Test
    fun neverConnectsToAnAddressTheGuardRefuses() = runBlocking {
        server.enqueue(response(200, "secret"))
        val machine = FetchGuard(PublicAddresses::isPublic) { true }
        val rebinding = HostLookup { listOf(loopback) }
        assertEquals(Fetched.NotAllowed, fetcher(machine).fetch(url("/admin")))
        assertEquals(Fetched.NotAllowed, fetcher(machine).fetch("http://[::ffff:127.0.0.1]:${server.port}/admin"))
        assertEquals(Fetched.NotAllowed, fetcher(machine).fetch("http://localhost:${server.port}/admin"))
        assertEquals(Fetched.NotAllowed, fetcher(machine).fetch("http://2130706433:${server.port}/admin"))
        assertEquals(Fetched.NotAllowed, fetcher(machine).fetch("http://[64:ff9b::127.0.0.1]:${server.port}/admin"))
        assertEquals(Fetched.NotAllowed, fetcher(machine, rebinding).fetch("http://public.test:${server.port}/admin"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun connectsOnlyToTheAddressItsResolverChecked() = runBlocking {
        val lookups = AtomicInteger()
        val docs = HostLookup { host ->
            lookups.incrementAndGet()
            if (host == "docs.test") listOf(loopback) else throw UnknownHostException(host)
        }
        server.enqueue(response(200, "by name"))
        val document = assertIs<Fetched.Document>(fetcher(mockOnly, docs).fetch("http://docs.test:${server.port}/doc"))
        assertContentEquals("by name".encodeToByteArray(), document.bytes)
        assertEquals("docs.test:${server.port}", server.takeRequest().headers["Host"])
        // One lookup for the one connection: no second answer to rebind to.
        assertEquals(1, lookups.get())
    }

    @Test
    fun refusesWhatTheServersOwnGuardRefuses() = runBlocking {
        val fetcher = fetcher(FetchGuard.Public)
        for (url in listOf(url("/"), "http://127.0.0.1/", "http://169.254.169.254/latest/meta-data/", "ftp://example.com/", "nonsense")) {
            assertEquals(Fetched.NotAllowed, fetcher.fetch(url), url)
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun answersAnAddressThatRefusesTheConnectionAsUnreachable() = runBlocking {
        val closed = ServerSocket(0, 1, loopback).use { it.localPort }
        assertEquals(Fetched.Unreachable, fetcher().fetch("http://127.0.0.1:$closed/"))
    }

    @Test
    fun answersANameWithNoAddressAsUnreachable() = runBlocking {
        val none = HostLookup { host -> throw UnknownHostException(host) }
        assertEquals(Fetched.Unreachable, fetcher(FetchGuard.Anywhere, none).fetch("https://no-such-host.test/"))
    }

    @Test
    fun givesUpOnAnAddressThatDoesNotAnswerInTime() = runBlocking {
        server.enqueue(MockResponse.Builder().code(200).body("late").headersDelay(5, TimeUnit.SECONDS).build())
        val started = TimeSource.Monotonic.markNow()
        val fetcher = DocumentFetcher(FetchGuard.Anywhere, timeout = 500.milliseconds).also(fetchers::add)
        assertEquals(Fetched.Unreachable, fetcher.fetch(url("/slow")))
        assertTrue(started.elapsedNow() < 3.seconds, "gave up after ${started.elapsedNow()}")
    }

    @Test
    fun keepsOnlyTheTypeSubtypeAndCharsetOfAContentType() {
        val types = mapOf(
            "application/json" to "application/json",
            "application/json; charset=utf-8" to "application/json;charset=UTF-8",
            "text/csv; header=present; charset=ISO-8859-1" to "text/csv;charset=ISO-8859-1",
            "TEXT/XML; Charset=\"windows-1252\"" to "text/xml;charset=windows-1252",
            "application/yaml; boundary=x" to "application/yaml",
            "text/plain; charset=no-such-charset" to "text/plain",
            "text/html" to "text/html"
        )
        for ((header, type) in types) assertEquals(MediaType.parseMediaType(type), DocumentFetcher.documentType(header), header)
        for (header in listOf(null, "", "garbage", "*/*", "text/*", "/json", "text/")) {
            assertEquals(MediaType.APPLICATION_OCTET_STREAM, DocumentFetcher.documentType(header), header.toString())
        }
    }

    private fun fetcher(guard: FetchGuard = FetchGuard.Anywhere, lookup: HostLookup = HostLookup.System): DocumentFetcher =
        DocumentFetcher(guard, lookup, timeout = 10.seconds).also(fetchers::add)

    private fun url(path: String) = "http://127.0.0.1:${server.port}$path"

    private fun response(status: Int, body: String, vararg headers: Pair<String, String>): MockResponse {
        val builder = MockResponse.Builder().code(status).body(body)
        for ((name, value) in headers) builder.setHeader(name, value)
        return builder.build()
    }

    private fun redirect(status: Int, location: String) = MockResponse.Builder().code(status).setHeader("Location", location).build()
}
