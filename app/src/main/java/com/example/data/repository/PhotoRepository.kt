package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.BookPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [요구사항 4] PhotoRepository (도서/세션 미디어 영속성 제어)
 * - 책임: PhotoDao를 캡슐화하여 아이의 독서 순간 스냅샷 이미지 및 도서 확인증 표지 파일 정보의 CRUD 및 Room 매핑 전담.
 * - 설계 안전성 (시니어 아키텍트 코멘트):
 *   1. 느린 디스크 IO 격리: 비대한 멀티미디어 파일 메타데이터 및 이미지 URIs 정보의 SQLite 영속화 및 삭제 작업을
 *      [Dispatchers.IO] 컨텍스트에 격리시켜 주입 및 수행하여 호출부의 Non-blocking 동작을 전적으로 보장합니다.
 *   2. Flow 반응형 도출: 독서 기록 데이터의 실시간 동기화를 위해 변경 이벤트를 감지하고 관찰 스트림(Flow)으로 반환합니다.
 */
class PhotoRepository(private val database: AppDatabase) {
    private val photoDao = database.photoDao()

    /**
     * 특정 도서에 속한 모든 사진 스트림을 가져옵니다.
     */
    fun getPhotosForBook(bookId: Int): Flow<List<BookPhoto>> = 
        photoDao.getPhotosForBook(bookId)

    /**
     * 특정 독서 세션에 속한 모든 사진 스트림을 가져옵니다.
     */
    fun getPhotosForSession(sessionId: Int): Flow<List<BookPhoto>> = 
        photoDao.getPhotosForSession(sessionId)

    /**
     * 새로운 사진 기록을 데이터베이스에 추가합니다.
     */
    suspend fun insertPhoto(photo: BookPhoto): Long = withContext(Dispatchers.IO) {
        photoDao.insertPhoto(photo)
    }

    /**
     * 기존 사진의 메타데이터(메모, 회전각 등)를 갱신합니다.
     */
    suspend fun updatePhoto(photo: BookPhoto) = withContext(Dispatchers.IO) {
        photoDao.updatePhoto(photo)
    }

    /**
     * 사진 기록을 데이터베이스에서 물리적/논리적으로 제거합니다.
     */
    suspend fun deletePhoto(photo: BookPhoto) = withContext(Dispatchers.IO) {
        photoDao.deletePhoto(photo)
    }
}
