package com.example.exception

import java.lang.RuntimeException

class CordaBusinessException(message: String?) : RuntimeException(message)