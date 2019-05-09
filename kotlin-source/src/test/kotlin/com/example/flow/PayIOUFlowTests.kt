package com.example.flow

import com.example.state.IOUState
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class PayIOUFlowTests {
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
        listOf(a, b).forEach { it.registerInitiatedFlow(PayIOUFlow.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `when A pay B then the IOUState should be consumed`() {
        createIOU()

        val partyA = a.info.singleIdentity()
        val partyB = b.info.singleIdentity()

        val flow = PayIOUFlow.Initiator(10, partyB)
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()

        for (node in listOf(a, b)) {
            node.transaction {
                var ious = node.services.vaultService.queryBy<IOUState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED)).states
                assertEquals(1, ious.size)

                val recordedState = ious.single().state.data
                assertEquals(10, recordedState.value)
                assertEquals(partyA, recordedState.lender)
                assertEquals(partyB, recordedState.borrower)

                ious = node.services.vaultService.queryBy<IOUState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)).states
                assertEquals(0, ious.size)
            }
        }
    }

    @Test
    fun `when A pay partially B then another IOUState should be create discounting the payment value`() {
        createIOU()

        val partyB = b.info.singleIdentity()
        val partyA = a.info.singleIdentity()

        val flow = PayIOUFlow.Initiator(8, partyB)
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()

        for (node in listOf(a, b)) {
            node.transaction {
                var ious = node.services.vaultService.queryBy<IOUState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED)).states
                assertEquals(1, ious.size)

                val previousState = ious.single().state.data
                assertEquals(10, previousState.value)
                assertEquals(partyA, previousState.lender)
                assertEquals(partyB, previousState.borrower)

                ious = node.services.vaultService.queryBy<IOUState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)).states
                assertEquals(1, ious.size)

                val actualState = ious.single().state.data
                assertEquals(2, actualState.value)
                assertEquals(partyA, actualState.lender)
                assertEquals(partyB, actualState.borrower)
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
                assertEquals(10, recordedState.value)
                assertEquals(a.info.singleIdentity(), recordedState.lender)
                assertEquals(b.info.singleIdentity(), recordedState.borrower)
            }
        }
    }
}