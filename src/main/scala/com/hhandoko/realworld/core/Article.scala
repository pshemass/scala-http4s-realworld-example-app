package com.hhandoko.realworld.core

import java.time.OffsetDateTime

final case class Article(
  slug: String,
  title: String,
  description: String,
  body: String,
  tagList: Set[Tag],
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  favorited: Boolean,
  favoritesCount: Long,
  author: Author
)

final case class Comment(
  id: Int,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  body: String,
  author: Author
)
