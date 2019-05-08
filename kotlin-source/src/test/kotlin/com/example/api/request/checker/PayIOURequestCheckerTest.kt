package com.example.api.request.checker

import com.example.contract.IOUContract
import com.example.exception.CordaBusinessException
import com.example.flow.ExampleFlow
import com.example.flow.PayIOUFlow
import com.example.state.IOUState
import com.google.common.collect.Lists
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.singleIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockServices
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.ledger
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PayIOURequestCheckerTest {

    val ledgerServices = MockServices(listOf("com.example.schema", "com.example.contract"))
    val defaultParameters = DriverParameters(isDebug = true, startNodesInProcess = true, extraCordappPackagesToScan = listOf("com.example.schema", "com.example.contract"))
    val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test(expected = CordaBusinessException::class)
    fun `IOU value must be non-negative`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val (partyAHandle) = listOf(
                    startNode(providedName = bankA.name)
            ).map { it.getOrThrow() }

            PayIOURequestChecker(partyAHandle.rpc).check(-10, bankB.party.name)
        }
    }

    @Test(expected = CordaBusinessException::class)
    fun `party name must be not null`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val (partyAHandle) = listOf(
                    startNode(providedName = bankA.name)
            ).map { it.getOrThrow() }

            PayIOURequestChecker(partyAHandle.rpc).check(10, null)
        }
    }

    @Test(expected = CordaBusinessException::class)
    fun `party name must be valid`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val (partyAHandle) = listOf(
                    startNode(providedName = bankA.name)
            ).map { it.getOrThrow() }

            PayIOURequestChecker(partyAHandle.rpc).check(10, bankB.party.name)
        }
    }

    @Test(expected = CordaBusinessException::class)
    fun `there must be at least one IOU to be payed`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true, extraCordappPackagesToScan = listOf("com.example.schema"))) {
            val (partyAHandle) = listOf(
                    startNode(providedName = bankA.name),
                    startNode(providedName = bankB.name)
            ).map { it.getOrThrow() }

            PayIOURequestChecker(partyAHandle.rpc).check(10, bankB.name)
        }
    }

    //FIXME TESTE NÃO ESTÁ RODANDO CORRETAMENTE
    @Test(expected = CordaBusinessException::class)
    fun `there must be only one IOU to be payed`() {


        ledgerServices.ledger {
            transaction {
                output(IOUContract.IOU_CONTRACT_ID, IOUState(10, bankA.party, bankB.party))
                command(listOf(bankA.publicKey, bankB.publicKey), IOUContract.Commands.Create())
                verifies()
            }

            transaction {
                output(IOUContract.IOU_CONTRACT_ID, IOUState(10, bankA.party, bankB.party))
                command(listOf(bankA.publicKey, bankB.publicKey), IOUContract.Commands.Create())
                verifies()
            }
        }

        val states = ledgerServices.vaultService.queryBy(IOUState::class.java).states
        assertEquals(2, states.size)

        driver(defaultParameters) {
            val (partyAHandle) = listOf(
                    startNode(providedName = bankA.name),
                    startNode(providedName = bankB.name)
            ).map { it.getOrThrow() }


            PayIOURequestChecker(partyAHandle.rpc).check(10, bankB.name)
        }
    }

}