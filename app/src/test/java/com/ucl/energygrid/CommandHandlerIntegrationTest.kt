package com.ucl.energygrid

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import com.ucl.energygrid.data.model.Mine


@ExperimentalCoroutinesApi
class CommandHandlerIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var handler: CommandHandler
    private lateinit var dummyViewModel: CallingViewModel

    private val sites = listOf(
        Triple("1", "ref1", "Mine A"),
        Triple("2", "ref2", "Mine B"),
        Triple("3", "ref3", "Mine C"),
        Triple("4", "ref4", "Mine D"),
        Triple("5", "ref5", "Mine E"),
        Triple("6", "ref6", "Mine F")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dummyViewModel = mock()

        handler = CommandHandler(
            sites = sites,
            sitesPerPage = 2,
            getInfoByReference = { ref ->
                Mine(
                    name = "Mine $ref",
                    reference = ref,
                    status = "C",
                    easting = 0.0,
                    northing = 0.0,
                    localAuthority = null,
                    floodRiskLevel = null,
                    floodHistory = null,
                    energyDemandHistory = null,
                    trend = null,
                    forecastEnergyDemand = null,
                    note = null
                )
            }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `full integration flow`() = runTest(testDispatcher) {
        // 初始 stage
        assertEquals("selectSite", handler.currentStage)
        assertEquals(0, handler.currentPage)

        // 選第一個 site
        val select1 = handler.handleCommand("1", dummyViewModel)
        assertEquals("basicInfo", handler.currentStage)
        assertEquals("Mine A", handler.selectedMineName)
        assertEquals("Mine ref1", handler.selectedMineInfo?.name)
        assertTrue(select1.sendCommands.contains("menu"))

        // 返回選 site
        val back = handler.handleCommand("back", dummyViewModel)
        assertEquals("selectSite", handler.currentStage)
        assertEquals(0, handler.currentPage)
        assertTrue(back.sendCommands.contains("1"))

        // 下一頁翻頁
        val nextPage = handler.handleCommand("next", dummyViewModel)
        assertEquals(1, handler.currentPage)
        assertTrue(nextPage.sendCommands.contains("3")) // 第二頁顯示的 site

        // 上一頁翻回
        val prevPage = handler.handleCommand("previous", dummyViewModel)
        assertEquals(0, handler.currentPage)
        assertTrue(prevPage.sendCommands.contains("1"))

        // 打開 menu
        val menu = handler.handleCommand("menu", dummyViewModel)
        assertEquals("menu", handler.currentStage)
        assertTrue(menu.sendCommands.contains("basic info"))
        assertTrue(menu.sendCommands.contains("forecast energy demand"))

        // 接聽電話
        handler.handleCommand("accept", dummyViewModel)
        verify(dummyViewModel).acceptCall()

        // 拒接電話
        handler.handleCommand("reject", dummyViewModel)
        verify(dummyViewModel).rejectCall()

        // 輸入未知指令
        val unknown = handler.handleCommand("foobar", dummyViewModel)
        assertTrue(unknown.sendCommands.isEmpty())
    }
}