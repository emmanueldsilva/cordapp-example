package com.example.flow

import com.example.state.IOUState
import net.corda.core.contracts.UniqueIdentifier
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

class PayIOUFlowOldTests {
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
        listOf(a, b).forEach { it.registerInitiatedFlow(PayIOUFlowOld.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `when A pay B then the IOUState should be consumed`() {
        var uniqueIdentifier : UniqueIdentifier = createIOU()

        val flow = PayIOUFlowOld.Initiator(uniqueIdentifier)
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()

        for (node in listOf(a, b)) {
            node.transaction {
                var ious = node.services.vaultService.queryBy<IOUState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.CONSUMED)).states
                assertEquals(1, ious.size)

                val recordedState = ious.single().state.data
                assertEquals(recordedState.value, 1)
                assertEquals(recordedState.lender, a.info.singleIdentity())
                assertEquals(recordedState.borrower, b.info.singleIdentity())

                ious = node.services.vaultService.queryBy<IOUState>(QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)).states
                assertEquals(0, ious.size)
            }
        }
    }

    private fun createIOU() : UniqueIdentifier {
        val iouValue = 1
        val flow = ExampleFlow.Initiator(1, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()

        var stateIdentifier = UniqueIdentifier()
        // We check the recorded IOU in both vaults.
        for (node in listOf(a, b)) {
            node.transaction {
                val ious = node.services.vaultService.queryBy<IOUState>().states
                assertEquals(1, ious.size)

                val recordedState = ious.single().state.data
                assertEquals(recordedState.value, iouValue)
                assertEquals(recordedState.lender, a.info.singleIdentity())
                assertEquals(recordedState.borrower, b.info.singleIdentity())

                stateIdentifier = recordedState.linearId
            }
        }

        return stateIdentifier
    }
}