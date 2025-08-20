package com.ucl.energygrid

import com.ucl.energygrid.data.model.Mine
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CommandHandler(
    private val sites: List<Triple<String, String, String>>, // (label, ref, name)
    private val sitesPerPage: Int = 5,
    private val getInfoByReference: suspend (String) -> Mine?
) {
    var currentStage by mutableStateOf("selectSite")
        private set
    var selectedMineName by mutableStateOf<String?>(null)
        private set
    var selectedMineInfo by mutableStateOf<Mine?>(null)
        private set
    var currentPage by mutableStateOf(0)
        private set

    private val maxPage: Int get() = if (sites.isEmpty()) 0 else (sites.size - 1) / sitesPerPage

    data class CommandResult(
        val sendCommands: List<String> = emptyList()
    )

    suspend fun handleCommand(command: String, viewModel: CallingViewModel): CommandResult {
        if (sites.isEmpty()) return CommandResult()

        when (command.lowercase()) {
            "menu" -> {
                currentStage = "menu"
                return CommandResult(listOf("reselect site", "basic info", "flooding trend",
                    "historical energy demand", "forecast energy demand", "back", "menu"))
            }

            "close menu" -> {
                currentStage = "selectSite"
                return CommandResult(listOf("menu"))
            }

            "reselect site" -> {
                currentStage = "selectSite"
                selectedMineName = null
                selectedMineInfo = null
                currentPage = 0
                return CommandResult(sites.map { it.first.lowercase() } + listOf("menu"))
            }

            "basic info" -> {
                currentStage = "basicInfo"
                return CommandResult(listOf("back", "menu"))
            }

            "flooding trend" -> {
                currentStage = "floodTrend"
                return CommandResult(listOf("back", "menu"))
            }

            "historical energy demand" -> {
                currentStage = "historicalEnergy"
                return CommandResult(listOf("back", "menu"))
            }

            "forecast energy demand" -> {
                currentStage = "forecastEnergy"
                return CommandResult(listOf("back", "menu"))
            }

            "back" -> {
                currentStage = "selectSite"
                return CommandResult(sites.map { it.first.lowercase() } + listOf("menu"))
            }

            "accept" -> {
                viewModel.acceptCall()
                return CommandResult()
            }

            "reject" -> {
                viewModel.rejectCall()
                return CommandResult()
            }

            "next" -> {
                if (currentStage == "selectSite" && currentPage < maxPage) {
                    currentPage += 1
                    return CommandResult(getCurrentPageCommands() + listOf("previous", "next"))
                }
            }

            "previous" -> {
                if (currentStage == "selectSite" && currentPage > 0) {
                    currentPage -= 1
                    return CommandResult(getCurrentPageCommands() + listOf("previous", "next"))
                }
            }
        }

        // 選 site
        if (currentStage == "selectSite") {
            val visibleSites = sites.drop(currentPage * sitesPerPage).take(sitesPerPage)
            val selectedSite = visibleSites.firstOrNull {
                it.first.equals(command, ignoreCase = true) ||
                        (command in listOf("one", "1", "two", "2", "three", "3",
                            "four", "4", "five", "5") && it.first.endsWith(command.takeLast(1)))
            }
            if (selectedSite != null) {
                selectedMineName = selectedSite.third
                selectedMineInfo = getInfoByReference(selectedSite.second)
                currentStage = "basicInfo"
                return CommandResult(listOf("back", "menu"))
            }
        }

        return CommandResult()
    }

    private fun getCurrentPageCommands(): List<String> {
        return sites.drop(currentPage * sitesPerPage).take(sitesPerPage).map { it.first.lowercase() }
    }
}