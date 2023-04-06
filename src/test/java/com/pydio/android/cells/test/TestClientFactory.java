package com.pydio.android.cells.test;

import androidx.annotation.NonNull;

import com.pydio.android.cells.transfer.CellsS3Client;
import com.pydio.cells.api.CustomEncoder;
import com.pydio.cells.api.Server;
import com.pydio.cells.api.Store;
import com.pydio.cells.api.Transport;
import com.pydio.cells.client.CellsClient;
import com.pydio.cells.client.ClientFactory;
import com.pydio.cells.transport.CellsTransport;
import com.pydio.cells.transport.auth.CredentialService;
import com.pydio.cells.utils.JavaCustomEncoder;
import com.pydio.cells.utils.MemoryStore;
import com.pydio.cells.utils.tests.TestCredentialService;

public class TestClientFactory extends ClientFactory {

    public TestClientFactory() {
        this(new TestCredentialService(new MemoryStore<>(), new MemoryStore<>()),
                new MemoryStore<>(), new MemoryStore<>());
    }

    public TestClientFactory(CredentialService credentialService, Store<Server> serverStore, Store<Transport> transportStore) {
        super(credentialService, serverStore, transportStore);
    }

    @Override
    protected CellsClient getCellsClient(@NonNull CellsTransport transport) {
        return new CellsClient(transport, new CellsS3Client(transport));
    }

    @Override
    public CustomEncoder getEncoder() {
        return new JavaCustomEncoder();
    }

}