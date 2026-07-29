package com.example

import com.example.model.AppSetting
import com.example.model.BlockedNumber
import com.example.model.CallNote
import com.example.model.CallRecording
import com.example.model.QuickResponse
import com.example.model.SpeedDial
import kotlinx.coroutines.flow.Flow

fun DialerRepository.getBlockedNumbers(): Flow<List<BlockedNumber>> = dao.getBlockedNumbersFlow()
suspend fun DialerRepository.addBlockedNumber(number: String) = dao.insertBlockedNumber(BlockedNumber(number))
suspend fun DialerRepository.removeBlockedNumber(number: String) = dao.deleteBlockedNumber(BlockedNumber(number))
suspend fun DialerRepository.isBlocked(number: String): Boolean = CallBlockerService.isNumberBlocked(dao, number)

suspend fun DialerRepository.exportBackup(password: String = ""): String = com.example.data.BackupRestoreManager.exportBackup(context, password)
suspend fun DialerRepository.importBackup(rawData: String, password: String = ""): Boolean = com.example.data.BackupRestoreManager.importBackup(context, rawData, password)

suspend fun DialerRepository.getServiceHealth(): ServiceHealth = CallBlockerService.getServiceHealthStatus(context)

fun DialerRepository.getSpeedDial(): Flow<List<SpeedDial>> = dao.getSpeedDialFlow()
suspend fun DialerRepository.saveSpeedDial(key: Int, number: String, name: String) = dao.insertSpeedDial(SpeedDial(key, number, name))
suspend fun DialerRepository.deleteSpeedDial(key: Int) = dao.deleteSpeedDial(key)

fun DialerRepository.getQuickResponses(): Flow<List<QuickResponse>> = dao.getQuickResponsesFlow()
suspend fun DialerRepository.addQuickResponse(message: String) = dao.insertQuickResponse(QuickResponse(message = message))
suspend fun DialerRepository.deleteQuickResponse(response: QuickResponse) = dao.deleteQuickResponse(response)

suspend fun DialerRepository.getVoicemailNumber(): String = dao.getSetting("voicemail_number") ?: ""
suspend fun DialerRepository.saveVoicemailNumber(number: String) = dao.insertSetting(AppSetting("voicemail_number", number))

suspend fun DialerRepository.getPreferredSim(): String = dao.getSetting("preferred_sim") ?: "Ask"
suspend fun DialerRepository.savePreferredSim(sim: String) = dao.insertSetting(AppSetting("preferred_sim", sim))

suspend fun DialerRepository.getCallNote(number: String): CallNote? = dao.getCallNote(number)
fun DialerRepository.getAllCallNotes(): Flow<List<CallNote>> = dao.getAllCallNotesFlow()
suspend fun DialerRepository.saveCallNote(number: String, note: String) = dao.insertCallNote(CallNote(number, note))
suspend fun DialerRepository.deleteCallNote(number: String) = dao.deleteCallNote(number)

fun DialerRepository.getAllCallRecordings(): Flow<List<CallRecording>> = dao.getAllCallRecordingsFlow()
suspend fun DialerRepository.saveCallRecording(recording: CallRecording) = dao.insertCallRecording(recording)
suspend fun DialerRepository.deleteCallRecording(id: Int) = dao.deleteCallRecording(id)
