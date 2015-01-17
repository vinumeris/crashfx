package controllers

import javax.inject.Singleton
import ninja.Context
import com.google.inject.persist.Transactional
import ninja.Result
import ninja.Results
import models.Crash
import javax.inject.Inject
import com.google.inject.Provider
import javax.persistence.EntityManager

Singleton
public open class CrashController [Inject] (open val em: Provider<EntityManager>) {
    Transactional
    public open fun crashUpload(context: Context): Result {
        context.getInputStream().use {
            val buffer = ByteArray(1024*1024);   // 1mb upload buffer
            val size = it.read(buffer)
            val str = String(buffer, 0, size, Charsets.UTF_8)
            saveLog(str)
        }
        return Results.noContent()
    }

    private class ExtractedData(val typeName: String, val appID: String)

    public open fun saveLog(str: String) {
        val data = extractExceptionTypeName(str)
        val crash = if (data != null)
            Crash(log = str, exceptionTypeName = data.typeName, appID = data.appID)
        else
            Crash(log = str)
        em.get().persist(crash)
    }

    public open fun extractExceptionTypeName(log: String): ExtractedData? {
        if (!log.startsWith("Crash at"))
            return null
        val lines = log.split("\n")
        if (lines.size() < 3)
            return null
        val firstExLine = lines[2]
        val typeName = firstExLine.split(':')[0].trim()
        val appID = lines[1]
        return ExtractedData(typeName, appID)
    }
}