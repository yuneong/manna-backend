package com.manna.common.exception

class MannaException(val errorCode: ErrorCode) : RuntimeException(errorCode.message)
