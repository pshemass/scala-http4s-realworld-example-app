package com.hhandoko.realworld.core

import java.time.OffsetDateTime

case class Comment(id: Int, createdAt: OffsetDateTime, updatedAt: OffsetDateTime, body: String, author: Author)
