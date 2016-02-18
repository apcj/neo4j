/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge.raft.replication.token;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

public class ReplicatedTokenHolderTest
{
//    final int EXPECTED_TOKEN_ID = 1;
//    final int INJECTED_TOKEN_ID = 1024;
//    Dependencies dependencies = mock( Dependencies.class );
//
//    long TIMEOUT_MILLIS = 1000;
//
//    @Before
//    public void setup()
//    {
//        NeoStores neoStore = mock( NeoStores.class );
//        LabelTokenStore labelTokenStore = mock( LabelTokenStore.class );
//        when( neoStore.getLabelTokenStore() ).thenReturn( labelTokenStore );
//        when( labelTokenStore.allocateNameRecords( Matchers.<byte[]>any() ) ).thenReturn( singletonList( new
//                DynamicRecord( 1l ) ) );
//        when( dependencies.resolveDependency( NeoStores.class ) ).thenReturn( neoStore );
//        when( dependencies.resolveDependency( TransactionRepresentationCommitProcess.class ) ).thenReturn( mock(
//                TransactionRepresentationCommitProcess.class ) );
//    }
//
//    @Test
//    public void shouldCreateTokenId() throws Exception
//    {
//        // given
//        IdGeneratorFactory idGeneratorFactory = mock( IdGeneratorFactory.class );
//        IdGenerator idGenerator = mock( IdGenerator.class );
//        when( idGenerator.nextId() ).thenReturn( 1L );
//
//        when( idGeneratorFactory.get( any( IdType.class ) ) ).thenReturn( idGenerator );
//
//        StateMachines stateMachines = new StateMachines();
//        Replicator replicator = new DirectReplicator( stateMachines );
//
//        when( dependencies.resolveDependency( TransactionRepresentationCommitProcess.class ) )
//                .thenReturn( mock( TransactionRepresentationCommitProcess.class ) );
//        StorageEngine storageEngine = mockedStorageEngine();
//        when( dependencies.resolveDependency( StorageEngine.class ) ).thenReturn( storageEngine );
//
//        ReplicatedTokenHolder<Token, LabelTokenRecord> tokenHolder = new ReplicatedLabelTokenHolder( replicator,
//                idGeneratorFactory, dependencies, TIMEOUT_MILLIS, NullLogProvider.getInstance() );
//
//        stateMachines.add( tokenHolder );
//
//        tokenHolder.setLastCommittedIndex( -1 );
//        tokenHolder.start();
//
//        // when
//        int tokenId = tokenHolder.getOrCreateId( "Person" );
//
//        // then
//        assertEquals( EXPECTED_TOKEN_ID, tokenId );
//    }
//
//    @Test
//    public void shouldTimeoutIfTokenDoesNotReplicateWithinTimeout() throws Exception
//    {
//        // given
//        IdGeneratorFactory idGeneratorFactory = mock( IdGeneratorFactory.class );
//        IdGenerator idGenerator = mock( IdGenerator.class );
//        when( idGenerator.nextId() ).thenReturn( 1L );
//
//        when( idGeneratorFactory.get( any( IdType.class ) ) ).thenReturn( idGenerator );
//
//        Replicator replicator = new DropAllTheThingsReplicator();
//        when( dependencies.resolveDependency( TransactionRepresentationCommitProcess.class ) )
//                .thenReturn( mock( TransactionRepresentationCommitProcess.class ) );
//        StorageEngine storageEngine = mockedStorageEngine();
//        when( dependencies.resolveDependency( StorageEngine.class ) ).thenReturn( storageEngine );
//
//        ReplicatedTokenHolder<Token, LabelTokenRecord> tokenHolder = new ReplicatedLabelTokenHolder( replicator,
//                idGeneratorFactory, dependencies, 10, NullLogProvider.getInstance() );
//
//        tokenHolder.setLastCommittedIndex( -1 );
//        tokenHolder.start();
//
//        // when
//        try
//        {
//            tokenHolder.getOrCreateId( "Person" );
//            fail( "Token creation attempt should have timed out" );
//        }
//        catch ( TransactionFailureException ex )
//        {
//            // expected
//        }
//    }
//
//    @Test
//    public void shouldStoreRaftLogIndexInTransactionHeader() throws Exception
//    {
//        // given
//        int logIndex = 1;
//
//        IdGeneratorFactory idGeneratorFactory = mock( IdGeneratorFactory.class );
//        IdGenerator idGenerator = mock( IdGenerator.class );
//        when( idGenerator.nextId() ).thenReturn( 1L );
//        when( idGeneratorFactory.get( any( IdType.class ) ) ).thenReturn( idGenerator );
//
//        StubTransactionCommitProcess commitProcess = new StubTransactionCommitProcess( null, null );
//        when( dependencies.resolveDependency( TransactionRepresentationCommitProcess.class ) ).thenReturn(
//                commitProcess );
//        StorageEngine storageEngine = mockedStorageEngine();
//        when( dependencies.resolveDependency( StorageEngine.class ) ).thenReturn( storageEngine );
//
//        StateMachines stateMachines = new StateMachines();
//        ReplicatedTokenHolder<Token, LabelTokenRecord> tokenHolder = new ReplicatedLabelTokenHolder(
//                new DirectReplicator( stateMachines ), idGeneratorFactory, dependencies, TIMEOUT_MILLIS, NullLogProvider.getInstance() );
//        tokenHolder.setLastCommittedIndex( -1 );
//        stateMachines.add( tokenHolder );
//
//        // when
//        ReplicatedTokenRequest tokenRequest = new ReplicatedTokenRequest( LABEL, "Person",
//                createCommandBytes( createCommands( EXPECTED_TOKEN_ID ) ) );
//        tokenHolder.applyCommand( tokenRequest, logIndex );
//
//        // then
//        List<TransactionRepresentation> transactions = commitProcess.transactionsToApply;
//        assertEquals(1, transactions.size());
//        assertEquals(logIndex, decodeLogIndexFromTxHeader( transactions.get( 0 ).additionalHeader()) );
//    }
//
//    @Test
//    public void shouldStoreInitialTokens() throws Exception
//    {
//        // given
//        ReplicatedTokenHolder<Token, LabelTokenRecord> tokenHolder =
//                new ReplicatedLabelTokenHolder( null, null, dependencies, TIMEOUT_MILLIS, NullLogProvider.getInstance() );
//
//        // when
//        tokenHolder.setInitialTokens( asList( new Token( "name1", 1 ), new Token( "name2", 2 ) ) );
//
//        // then
//        assertThat( tokenHolder.getAllTokens(), hasItems( new Token( "name1", 1 ), new Token( "name2", 2 ) ) );
//    }
//
//    @Test
//    public void shouldThrowExceptionIfLastCommittedIndexNotSet() throws Exception
//    {
//        // given
//        ReplicatedTokenHolder<Token, LabelTokenRecord> tokenHolder = new ReplicatedLabelTokenHolder( null,
//                null, dependencies, TIMEOUT_MILLIS, NullLogProvider.getInstance() );
//
//        // when
//        try
//        {
//            tokenHolder.start();
//            fail( "Should have thrown exception" );
//        }
//        catch ( IllegalStateException e )
//        {
//            // expected
//        }
//    }
//
//    @Test
//    public void shouldGetExistingTokenIdFromAgesAgo() throws Exception
//    {
//        // given
//        IdGeneratorFactory idGeneratorFactory = mock( IdGeneratorFactory.class );
//        IdGenerator idGenerator = mock( IdGenerator.class );
//        when( idGenerator.nextId() ).thenReturn( 1024L );
//
//        when( idGeneratorFactory.get( any( IdType.class ) ) ).thenReturn( idGenerator );
//
//        StateMachines stateMachines = new StateMachines();
//        Replicator replicator = new DirectReplicator( stateMachines );
//
//        ReplicatedTokenHolder<Token, LabelTokenRecord> tokenHolder = new ReplicatedLabelTokenHolder( replicator,
//                idGeneratorFactory, dependencies, TIMEOUT_MILLIS, NullLogProvider.getInstance() );
//
//        stateMachines.add( tokenHolder );
//
//        tokenHolder.setLastCommittedIndex( -1 );
//        tokenHolder.start();
//        tokenHolder.applyCommand( new ReplicatedTokenRequest( LABEL, "Person", createCommandBytes(
//                createCommands( EXPECTED_TOKEN_ID ) ) ), 0 );
//
//        // when
//        int tokenId = tokenHolder.getOrCreateId( "Person" );
//
//        // then
//        assertEquals( EXPECTED_TOKEN_ID, tokenId );
//    }
//
//    @Test
//    public void shouldStoreAndReturnASingleTokenForTwoConcurrentRequests() throws Exception
//    {
//        // given
//        IdGeneratorFactory idGeneratorFactory = mock( IdGeneratorFactory.class );
//        IdGenerator idGenerator = mock( IdGenerator.class );
//        when( idGenerator.nextId() ).thenReturn( 1L );
//
//        when( idGeneratorFactory.get( any( IdType.class ) ) ).thenReturn( idGenerator );
//
//        StorageEngine storageEngine = mockedStorageEngine();
//        when( dependencies.resolveDependency( StorageEngine.class ) ).thenReturn( storageEngine );
//
//        StateMachines stateMachines = new StateMachines();
//        RaceConditionSimulatingReplicator replicator = new RaceConditionSimulatingReplicator(stateMachines);
//
//        ReplicatedTokenHolder<Token, LabelTokenRecord> tokenHolder = new ReplicatedLabelTokenHolder( replicator,
//                idGeneratorFactory, dependencies, TIMEOUT_MILLIS, NullLogProvider.getInstance() );
//        stateMachines.add( tokenHolder );
//
//        tokenHolder.setLastCommittedIndex( -1 );
//        tokenHolder.start();
//        replicator.injectLabelTokenBeforeOtherOneReplicates( new ReplicatedTokenRequest( LABEL, "Person",
//                createCommandBytes( createCommands( INJECTED_TOKEN_ID ) ) ) );
//
//        // when
//        int tokenId = tokenHolder.getOrCreateId( "Person" );
//
//        // then
//        assertEquals( INJECTED_TOKEN_ID, tokenId );
//    }
//
//    @Test
//    public void shouldStoreAndReturnASingleTokenForTwoDifferentConcurrentRequests() throws Exception
//    {
//        // given
//        IdGeneratorFactory idGeneratorFactory = mock( IdGeneratorFactory.class );
//        IdGenerator idGenerator = mock( IdGenerator.class );
//        when( idGenerator.nextId() ).thenReturn( 1L );
//
//        when( idGeneratorFactory.get( any( IdType.class ) ) ).thenReturn( idGenerator );
//
//        StorageEngine storageEngine = mockedStorageEngine();
//        when( dependencies.resolveDependency( StorageEngine.class ) ).thenReturn( storageEngine );
//
//        StateMachines stateMachines = new StateMachines();
//        RaceConditionSimulatingReplicator replicator = new RaceConditionSimulatingReplicator( stateMachines );
//
//        ReplicatedTokenHolder<Token, LabelTokenRecord> tokenHolder = new ReplicatedLabelTokenHolder( replicator,
//                idGeneratorFactory, dependencies, TIMEOUT_MILLIS, NullLogProvider.getInstance() );
//        stateMachines.add( tokenHolder );
//
//        tokenHolder.setLastCommittedIndex( -1 );
//        tokenHolder.start();
//        replicator.injectLabelTokenBeforeOtherOneReplicates( new ReplicatedTokenRequest( LABEL, "Dog",
//                createCommandBytes( createCommands( INJECTED_TOKEN_ID ) ) ) );
//
//        // when
//        int tokenId = tokenHolder.getOrCreateId( "Person" );
//
//        // then
//        assertEquals( EXPECTED_TOKEN_ID, tokenId );
//    }
//
//    private List<StorageCommand> createCommands( int tokenId )
//    {
//        List<StorageCommand> commands = new ArrayList<>();
//        commands.add(
//                new Command.LabelTokenCommand( new LabelTokenRecord( tokenId ), new LabelTokenRecord( tokenId ) ) );
//        return commands;
//    }
//
//    static class RaceConditionSimulatingReplicator implements Replicator
//    {
//        private final StateMachine stateMachine;
//        private ReplicatedTokenRequest otherToken;
//
//        public RaceConditionSimulatingReplicator(StateMachine stateMachine )
//        {
//            this.stateMachine = stateMachine;
//        }
//
//        public void injectLabelTokenBeforeOtherOneReplicates( ReplicatedTokenRequest token )
//        {
//            this.otherToken = token;
//        }
//
//        @Override
//        public void replicate( final ReplicatedContent content ) throws ReplicationFailedException
//        {
//            if ( otherToken != null )
//            {
//                stateMachine.applyCommand( otherToken, 0 );
//            }
//            stateMachine.applyCommand( content, 0 );
//        }
//
//    }
//
//    static class DropAllTheThingsReplicator implements Replicator
//    {
//        @Override
//        public void replicate( final ReplicatedContent content ) throws ReplicationFailedException
//        {
//        }
//    }
//
//    private class StubTransactionCommitProcess extends TransactionRepresentationCommitProcess
//    {
//        private final List<TransactionRepresentation> transactionsToApply = new ArrayList<>();
//
//        public StubTransactionCommitProcess( TransactionAppender appender, StorageEngine storageEngine )
//        {
//            super( appender, storageEngine );
//        }
//
//        @Override
//        public long commit( TransactionToApply batch, CommitEvent commitEvent, TransactionApplicationMode mode )
//                throws TransactionFailureException
//        {
//            transactionsToApply.add( batch.transactionRepresentation() );
//            return -1;
//        }
//    }
//
//    private StorageEngine mockedStorageEngine() throws Exception
//    {
//        StorageEngine storageEngine = mock( StorageEngine.class );
//        doAnswer( invocation -> {
//            Collection<StorageCommand> target = invocation.getArgumentAt( 0, Collection.class );
//            ReadableTransactionState txState = invocation.getArgumentAt( 1, ReadableTransactionState.class );
//            txState.accept( new TxStateVisitor.Adapter()
//            {
//                @Override
//                public void visitCreatedLabelToken( String name, int id )
//                {
//                    LabelTokenRecord before = new LabelTokenRecord( id );
//                    LabelTokenRecord after = before.clone();
//                    after.setInUse( true );
//                    target.add( new Command.LabelTokenCommand( before, after ) );
//                }
//            } );
//            return null;
//        } ).when( storageEngine ).createCommands( anyCollection(), any( ReadableTransactionState.class ),
//                any( ResourceLocker.class ), anyLong() );
//        return storageEngine;
//    }
}
