package com.example.api.request.checker

import com.example.exception.CordaBusinessException
import com.example.schema.IOUSchemaV1
import com.example.state.IOUState
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder

class PayIOURequestChecker(private val rpcOps: CordaRPCOps) {

    fun check(iouValue: Int, partyName: CordaX500Name?) {
        if (iouValue <= 0 ) {
            throw CordaBusinessException("Query parameter 'iouValue' must be non-negative.\n")
        }

        if (partyName == null) {
            throw CordaBusinessException("Query parameter 'partyName' missing or has wrong format.\n")
        }

        val otherParty = rpcOps.wellKnownPartyFromX500Name(partyName)?:
            throw CordaBusinessException("Party named $partyName cannot be found.\n")

        val me = rpcOps.nodeInfo().legalIdentities.single()
        val results = builder {
            val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
            val lenderCriteria = QueryCriteria.VaultCustomQueryCriteria(IOUSchemaV1.PersistentIOU::lenderName.equal(me.name.toString()))
            val borrowerCriteria = QueryCriteria.VaultCustomQueryCriteria(IOUSchemaV1.PersistentIOU::borrowerName.equal(otherParty.name.toString()))
            rpcOps.vaultQueryByCriteria(generalCriteria and lenderCriteria and borrowerCriteria, IOUState::class.java).states
        }

        if (results.isEmpty()) {
            throw CordaBusinessException("There are no IOUs to be payed")
        }

        if (results.size > 1) {
            throw CordaBusinessException("There are more than one IOUs created to the borrower $partyName")
        }
    }

}
