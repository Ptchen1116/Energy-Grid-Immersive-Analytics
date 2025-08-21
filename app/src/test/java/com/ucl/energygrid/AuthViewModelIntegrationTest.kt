package com.ucl.energygrid

import app.cash.turbine.test
import com.ucl.energygrid.data.remote.apis.UserApi
import com.ucl.energygrid.data.repository.UserRepository
import com.ucl.energygrid.ui.screen.AuthViewModel
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Base64


@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockWebServer: MockWebServer
    private lateinit var userApi: UserApi
    private lateinit var userRepository: UserRepository
    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setup() {
        // Set Main dispatcher for coroutine tests
        Dispatchers.setMain(testDispatcher)

        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()

        userApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserApi::class.java)

        userRepository = UserRepository(userApi)

        authViewModel = AuthViewModel(userRepository, dispatcher = testDispatcher)

        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getUrlDecoder().decode(firstArg<String>())
        }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        mockWebServer.shutdown()
    }
    @Test
    fun `login success updates state and emits message`() = runTest {
        val fakeJwtPayload = """{"sub": "user123"}"""
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"none","typ":"JWT"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(fakeJwtPayload.toByteArray())

        val fakeJwtToken = "$header.$payload."

        val loginResponse = """
            {
                "access_token": "$fakeJwtToken",
                "token_type": "bearer"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(loginResponse)
        )

        authViewModel.uiMessage.test {
            authViewModel.login("test@example.com", "password123")

            advanceUntilIdle()
            val message = awaitItem()
            assertTrue(message.contains("Login success"))
        }
    }

    @Test
    fun `login failure emits error message`() = runTest {
        val errorBody = """{"detail":"Invalid credentials"}"""
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorBody)
        )

        authViewModel.uiMessage.test {
            authViewModel.login("bad@example.com", "wrongpassword")

            advanceUntilIdle()

            assertFalse(authViewModel.isLoggedIn.value)
            assertNull(authViewModel.userId.value)
            assertNull(authViewModel.userToken.value)

            val message = awaitItem()
            assertTrue(message.contains("Login failed"))
        }
    }

    @Test
    fun `register success emits success message`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{}")
        )

        authViewModel.uiMessage.test {
            authViewModel.register("username", "email@test.com", "password")

            advanceUntilIdle()

            val message = awaitItem()
            assertTrue(message.contains("Register successful"))
        }
    }

    @Test
    fun `register failure emits error message`() = runTest {
        val errorBody = """{"detail":"Email already exists"}"""
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(errorBody)
        )

        authViewModel.uiMessage.test {
            authViewModel.register("username", "email@test.com", "password")

            advanceUntilIdle()

            val message = awaitItem()
            assertTrue(message.contains("Register failed"))
        }
    }
}