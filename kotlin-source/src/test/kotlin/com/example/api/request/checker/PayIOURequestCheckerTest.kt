package com.example.api.request.checker

import com.example.exception.CordaBusinessException
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import org.junit.Test

class PayIOURequestCheckerTest {

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
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            val (partyAHandle, partyBHandle) = listOf(
                    startNode(providedName = bankA.name),
                    startNode(providedName = bankB.name)
            ).map { it.getOrThrow() }

            PayIOURequestChecker(partyAHandle.rpc).check(10, bankB.party.name)
        }
    }

}