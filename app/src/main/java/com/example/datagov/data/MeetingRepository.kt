package com.example.datagov.data

import kotlinx.coroutines.flow.Flow

class MeetingRepository(private val meetingDao: MeetingDao) {

    val allMeetings: Flow<List<Meeting>> = meetingDao.getAllMeetings()

    suspend fun getMeetingById(id: Long): Meeting? {
        return meetingDao.getMeetingById(id)
    }

    suspend fun insertMeeting(meeting: Meeting): Long {
        return meetingDao.insertMeeting(meeting)
    }

    suspend fun updateMeeting(meeting: Meeting) {
        meetingDao.updateMeeting(meeting)
    }

    suspend fun deleteMeeting(meeting: Meeting) {
        meetingDao.deleteMeeting(meeting)
    }
}

