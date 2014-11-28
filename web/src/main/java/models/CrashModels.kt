package models

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob
import org.hibernate.annotations.Type
import java.sql.Timestamp
import java.time.Instant
import javax.persistence.GeneratedValue

Entity
open class Crash(
        [Id] [GeneratedValue] open var id: Long? = null,
        open var timestamp: Timestamp? = Timestamp.from(Instant.now()),
        [Lob] [Type(type = "org.hibernate.type.TextType")] open var log: String? = null,

        open var exceptionTypeName: String? = null,

        open var appID: String? = null
)