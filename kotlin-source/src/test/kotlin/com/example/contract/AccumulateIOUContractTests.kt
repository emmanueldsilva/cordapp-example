package com.example.contract

import com.example.contract.IOUContract.Companion.IOU_CONTRACT_ID
import com.example.state.IOUState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class AccumulateIOUTests {
    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))
    private val iouValue = 1

    @Test
    fun `transaction must include Accumulate command`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Accumulate())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have one input`() {
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Accumulate())
                `fails with`("Only one input state should be consumed when an IOU will be accumulated.")
            }
        }
    }

    @Test
    fun `transaction must have only one input`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Accumulate())
                `fails with`("Only one input state should be consumed when an IOU will be accumulated.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Accumulate())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `transaction must have only one output`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Accumulate())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `lender must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(miniCorp.publicKey, IOUContract.Commands.Accumulate())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `borrower must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(megaCorp.publicKey, IOUContract.Commands.Accumulate())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `lender is not borrower`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, megaCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, megaCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Accumulate())
                `fails with`("The lender and the borrower cannot be the same entity.")
            }
        }
    }

    @Test
    fun `input's lender is the same of output's lender`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, megaCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Accumulate())
                `fails with`("The input's lender and the output's lender should be the same entity.")
            }
        }
    }

    @Test
    fun `input's borrower is the same of output's borrower`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, miniCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Accumulate())
                `fails with`("The input's borrower and the output's borrower should be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-value IOUs`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(-1, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Accumulate())
                `fails with`("The IOU's value must be non-negative.")
            }
        }
    }

}