package com.example.flow

import com.example.state.IOUState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Matchers
import org.mockito.internal.hamcrest.HamcrestArgumentMatcher
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccumulateIOUFlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.example.contract", "com.example.schema"))
        a = network.createPartyNode()
        b = network.createPartyNode()

        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b).forEach { it.registerInitiatedFlow(ExampleFlow.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `when A create another IOU of 5 then the IOU's accumulated value should be of 15`() {
        createIOU()

        val partyA = a.info.singleIdentity()
        val partyB = b.info.singleIdentity()

        val anotherIOUValue = 5
        val flow = ExampleFlow.Initiator(anotherIOUValue, partyB)
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()

        for (node in listOf(a, b)) {
            node.transaction {
                var ious = node.services.vaultService.queryBy<IOUState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED)).states
                assertEquals(1, ious.size)

                val consumedState = ious.single().state.data
                assertEquals(10, consumedState.value)
                assertEquals(partyA, consumedState.lender)
                assertEquals(partyB, consumedState.borrower)

                ious = node.services.vaultService.queryBy<IOUState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)).states
                assertEquals(1, ious.size)

                val unconsumedState = ious.single().state.data
                assertEquals(15, unconsumedState.value)
                assertEquals(partyA, unconsumedState.lender)
                assertEquals(partyB, unconsumedState.borrower)
            }
        }
    }

    private fun createIOU() {
        val iouValue = 10
        val flow = ExampleFlow.Initiator(iouValue, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()

        // We check the recorded IOU in both vaults.
        for (node in listOf(a, b)) {
            node.transaction {
                val ious = node.services.vaultService.queryBy<IOUState>().states
                assertEquals(1, ious.size)

                val recordedState = ious.single().state.data
                assertEquals(iouValue, recordedState.value)
                assertEquals(a.info.singleIdentity(), recordedState.lender)
                assertEquals(b.info.singleIdentity(), recordedState.borrower)
            }
        }
    }
}