package com.example.api.request.checker

import com.example.exception.CordaBusinessException
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps

class CreateIOURequestChecker(private val rpcOps: CordaRPCOps) {

    fun check(iouValue: Int, partyName: CordaX500Name?) {
        if (iouValue <= 0 ) {
            throw CordaBusinessException("Query parameter 'iouValue' must be non-negative.\n")
        }

        if (partyName == null) {
            throw CordaBusinessException("Query parameter 'partyName' missing or has wrong format.\n")
        }

        if (rpcOps.wellKnownPartyFromX500Name(partyName) == null) {
            throw CordaBusinessException("Party named $partyName cannot be found.\n")
        }
    }

}
