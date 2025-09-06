package com.ucl.energygrid.com.ucl.energygrid

import app.cash.turbine.test
import com.ucl.energygrid.data.repository.UserRepository
import com.ucl.energygrid.ui.screen.AuthViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
        authViewModel = AuthViewModel(userRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login success updates state and emits success message`() = runTest(testDispatcher) {
        val fakeUserId = "user123"
        val fakeToken = "tokenABC"
        coEvery { userRepository.login("email@test.com", "password") } returns Result.success(fakeUserId to fakeToken)

        authViewModel.uiMessage.test {
            authViewModel.login("email@test.com", "password")
            advanceUntilIdle()

            assertTrue(authViewModel.isLoggedIn.value)
            assertEquals(fakeUserId, authViewModel.userId.value)
            assertEquals(fakeToken, authViewModel.userToken.value)

            val message = awaitItem()
            assertTrue(message.contains("Login success"))
        }
    }

    @Test
    fun `login failure updates state and emits error message`() = runTest {
        val errorMsg = "Invalid credentials"
        coEvery { userRepository.login(any(), any()) } returns Result.failure(Exception(errorMsg))

        authViewModel.uiMessage.test {
            authViewModel.login("bademail@test.com", "badpassword")
            advanceUntilIdle()

            assertFalse(authViewModel.isLoggedIn.value)
            assertEquals(null, authViewModel.userId.value)
            assertEquals(null, authViewModel.userToken.value)

            val message = awaitItem()
            assertTrue(message.contains("Login failed"))
        }
    }

    @Test
    fun `register success emits success message`() = runTest {
        coEvery { userRepository.register(any(), any(), any()) } returns Result.success(Unit)

        authViewModel.uiMessage.test {
            authViewModel.register("username", "email@test.com", "password")
            advanceUntilIdle()

            val message = awaitItem()
            assertTrue(message.contains("Register successful"))
        }
    }

    @Test
    fun `register failure emits error message`() = runTest {
        val errorMsg = "Email already exists"
        coEvery { userRepository.register(any(), any(), any()) } returns Result.failure(Exception(errorMsg))

        authViewModel.uiMessage.test {
            authViewModel.register("username", "email@test.com", "password")
            advanceUntilIdle()

            val message = awaitItem()
            assertTrue(message.contains("Register failed"))
        }
    }

    @Test
    fun `logout clears state`() {
        authViewModel.logout()

        assertFalse(authViewModel.isLoggedIn.value)
        assertEquals(null, authViewModel.userId.value)
        assertEquals(null, authViewModel.userToken.value)
    }
}