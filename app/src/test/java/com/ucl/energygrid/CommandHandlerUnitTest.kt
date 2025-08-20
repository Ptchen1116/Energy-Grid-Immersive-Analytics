import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
import com.ucl.energygrid.CommandHandler
import com.ucl.energygrid.CallingViewModel
import com.ucl.energygrid.data.model.Mine


@ExperimentalCoroutinesApi
class CommandHandlerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var handler: CommandHandler
    private lateinit var dummyViewModel: CallingViewModel

    private val sites = listOf(
        Triple("1", "ref1", "Mine A"),
        Triple("2", "ref2", "Mine B"),
        Triple("3", "ref3", "Mine C")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 使用 Mockito 生成 mock ViewModel
        dummyViewModel = mock()

        handler = CommandHandler(
            sites = sites,
            sitesPerPage = 2,
            getInfoByReference = { ref: String ->
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
    fun `menu command sets stage to menu`() = runTest(testDispatcher) {
        val result = handler.handleCommand("menu", dummyViewModel)
        assertEquals("menu", handler.currentStage)
        assertTrue(result.sendCommands.contains("basic info"))
    }

    @Test
    fun `reselect site resets selection`() = runTest(testDispatcher) {
        handler.handleCommand("1", dummyViewModel) // select first mine
        assertEquals("basicInfo", handler.currentStage)

        val result = handler.handleCommand("reselect site", dummyViewModel)
        assertEquals("selectSite", handler.currentStage)
        assertNull(handler.selectedMineName)
        assertNull(handler.selectedMineInfo)
        assertTrue(result.sendCommands.contains("1"))
    }

    @Test
    fun `next and previous pagination works`() = runTest(testDispatcher) {
        assertEquals(0, handler.currentPage)
        handler.handleCommand("next", dummyViewModel)
        assertEquals(1, handler.currentPage)
        handler.handleCommand("previous", dummyViewModel)
        assertEquals(0, handler.currentPage)
    }

    @Test
    fun `accept and reject call updates viewModel`() = runTest(testDispatcher) {
        handler.handleCommand("accept", dummyViewModel)
        verify(dummyViewModel).acceptCall()

        handler.handleCommand("reject", dummyViewModel)
        verify(dummyViewModel).rejectCall()
    }

    @Test
    fun `select site by number updates mine info`() = runTest(testDispatcher) {
        handler.handleCommand("1", dummyViewModel)
        assertEquals("Mine ref1", handler.selectedMineInfo?.name)
        assertEquals("Mine A", handler.selectedMineName)
        assertEquals("basicInfo", handler.currentStage)
    }

    @Test
    fun `unknown command returns empty sendCommands`() = runTest(testDispatcher) {
        val result = handler.handleCommand("unknown", dummyViewModel)
        assertTrue(result.sendCommands.isEmpty())
    }
}
