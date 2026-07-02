package com.paymentslab.backend

import io.ktor.http.HttpStatusCode

/**
 * Domain exceptions mapped to [com.paymentslab.core.protocol.ApiError] + an HTTP status by the
 * StatusPages plugin. [code] is the stable machine-readable ApiError.code.
 */
sealed class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
) : RuntimeException(message)

class BadRequestException(code: String, message: String) :
    ApiException(HttpStatusCode.BadRequest, code, message)

class NotFoundException(code: String, message: String) :
    ApiException(HttpStatusCode.NotFound, code, message)

class UnauthorizedException(code: String, message: String) :
    ApiException(HttpStatusCode.Unauthorized, code, message)
