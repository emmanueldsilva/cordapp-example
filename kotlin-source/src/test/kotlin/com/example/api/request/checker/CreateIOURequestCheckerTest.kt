package com.example.api.request.checker

import com.example.exception.CordaBusinessException
import com.example.flow.ExampleFlow
import com.example.flow.PayIOUFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test

class CreateIOURequestCheckerTest {

    val bankA = TestIdentity(CordaX500Name("BankA", "", "GB"))
    val bankB = TestIdentity(CordaX500Name("BankB", "", "US"))

    @Test(expected = CordaBusinessException::class)
    fun `IOU value must be non-negative`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val (partyAHandle) = listOf(
                    startNode(providedName = bankA.name)
            ).map { it.getOrThrow() }

            CreateIOURequestChecker(partyAHandle.rpc).check(-10, bankB.party.name)
        }
    }

    @Test(expected = CordaBusinessException::class)
    fun `party name must be not null`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val (partyAHandle) = listOf(
                    startNode(providedName = bankA.name)
            ).map { it.getOrThrow() }

            CreateIOURequestChecker(partyAHandle.rpc).check(10, null)
        }
    }

    @Test(expected = CordaBusinessException::class)
    fun `party name must be valid`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val (partyAHandle) = listOf(
                    startNode(providedName = bankA.name)
            ).map { it.getOrThrow() }

            CreateIOURequestChecker(partyAHandle.rpc).check(10, bankB.party.name)
        }
    }

}