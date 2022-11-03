package com.example.mmp3.rsocket



import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.utils.io.core.*
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.core.MimeType
import io.rsocket.kotlin.core.WellKnownMimeType
import io.rsocket.kotlin.keepalive.KeepAlive
import io.rsocket.kotlin.ktor.client.RSocketSupport
import io.rsocket.kotlin.ktor.client.rSocket
import io.rsocket.kotlin.metadata.RoutingMetadata
import io.rsocket.kotlin.metadata.buildCompositeMetadata
import io.rsocket.kotlin.metadata.metadata
import io.rsocket.kotlin.metadata.security.AuthMetadata
import io.rsocket.kotlin.metadata.security.BearerAuthMetadata
import io.rsocket.kotlin.metadata.toPacket
import io.rsocket.kotlin.payload.PayloadMimeType
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class LiveSessionClient(
    private val exceptionHandler: ExceptionHandler,
    private val userIdProvider: () -> String,
    private val baseUrl: String,
    private val dateFormat: SimpleDateFormat,
    private val tokenUserHandler: TokenUserHandler
) {
    private val gson = Gson()
    private lateinit var httpClient: HttpClient
    private val liveSessionIdFlow = MutableStateFlow<String?>(null)

    @Volatile
    private var rSocketClient: RSocket? = null
    private val mutex = Mutex()
    fun connect(liveSessionId: String) {
        liveSessionIdFlow.value = liveSessionId
        httpClient = HttpClient(OkHttp) {
            install(WebSockets)
            install(RSocketSupport) {
                connector {
                    maxFragmentSize = 1024
                    connectionConfig {
                        keepAlive = KeepAlive(
                            interval = 30.seconds,
                            maxLifetime = 2.minutes
                        )
                        //payload for setup frame

                        setupPayload {
                            buildPayload {
                                metadata(buildCompositeMetadata {
                                    add(BearerAuthMetadata(tokenUserHandler.sessionId!!))})
                                data(
                                    gson.toJson(
                                        Connection(
                                            userIdProvider(),
                                            liveSessionIdFlow.value!!
                                        )
                                    )
                                )
                            }
                        }
//                        mime types
                        payloadMimeType = PayloadMimeType(
                            data = WellKnownMimeType.ApplicationJson,
                            metadata = WellKnownMimeType.MessageRSocketCompositeMetadata
                        )
                        acceptor {
                            RSocketRequestHandler {
                                requestResponse { it }
                            }
                        }
                    }
                }
            }
        }
    }


    suspend fun commentsCount(): Flow<CommentsCount> =
        requestStream("commentsCounter/${liveSessionIdFlow.value!!}")

    suspend fun comments(): Flow<Comment> =
        requestStream("comments/${liveSessionIdFlow.value!!}")

    suspend fun views(): Flow<ViewsCount> =
        requestStream("views/${liveSessionIdFlow.value!!}")

    suspend fun sendComment(content: String, fullName: String, image: String): Comment =
        requestResponse(
            "sendComment/${liveSessionIdFlow.value!!}",
            Comment(
                "${UUID.randomUUID()}",
                liveSessionIdFlow.value!!,
                userIdProvider(),
                content = content,
                date = dateFormat.format(Date()),
                fullName = fullName,
                image = image
            )
        )

    suspend fun askForLike(): Like? =
        requestResponse("likeForUser/${liveSessionIdFlow.value!!}", userIdProvider()) {
            it
        }


    suspend fun sendLike(likeType: LikeType): Like =
        requestResponse(
            "sendLike/${liveSessionIdFlow.value!!}",
            Like("${UUID.randomUUID()}", liveSessionIdFlow.value!!, userIdProvider(), likeType)
        )

    suspend fun likes(): Flow<LikesCount> =
        requestStream("likesCounter/${liveSessionIdFlow.value!!}")

    private suspend fun rSocket(): RSocket {
        mutex.lock()
        if (rSocketClient != null) {
            mutex.unlock()
        } else {
            rSocketClient = httpClient.rSocket(baseUrl)
            mutex.unlock()
        }
        return rSocketClient!!
    }

    private suspend inline fun <reified T> requestStream(routing: String): Flow<T> {
        Log.d(TAG, "requestStream: $routing")
        val rSocket = rSocket()
        val md = buildRoutingMetaData(routing)
        return rSocket.requestStream(
            buildPayload
            {
                metadata(md.toPacket())
                data(ByteReadPacket.Empty)
            }
        ).map { gson.fromJson(it.data.readText(), T::class.java) }
    }

    private suspend inline fun <reified T, reified R> requestResponse(
        routing: String,
        payload: T,
        dataBuilder: (T) -> String = gson::toJson
    ): R {
        Log.d(TAG, "requestResponse: $routing")
        val rSocket = rSocket()
        val md = buildRoutingMetaData(routing)
        return rSocket.requestResponse(
            buildPayload
            {
                metadata(md.toPacket())
                data(dataBuilder(payload))
            }
        ).let {
            Log.d(TAG, "requestResponse: $it ")
            gson.fromJson(it.data.readText(), R::class.java)
        }
    }


    private fun buildRoutingMetaData(routing: String) = buildCompositeMetadata {
        add(RoutingMetadata(routing))
    }

    fun close() {
        httpClient.close()
    }
}

private const val TAG = "LivePlayerViewModel"