package com.pydio.android.cells.test;

import com.pydio.android.cells.services.S3Client;
import com.pydio.cells.api.CustomEncoder;
import com.pydio.cells.api.Server;
import com.pydio.cells.api.Store;
import com.pydio.cells.api.Transport;
import com.pydio.cells.client.CellsClient;
import com.pydio.cells.client.ClientFactory;
import com.pydio.cells.transport.CellsTransport;
import com.pydio.cells.transport.auth.CredentialService;
import com.pydio.cells.utils.JavaCustomEncoder;

public class TestClientFactory extends ClientFactory {

    public TestClientFactory() {
        super();
    }

    public TestClientFactory(CredentialService credentialService, Store<Server> serverStore, Store<Transport> transportStore) {
        super(credentialService, serverStore, transportStore);
    }

    @Override
    protected CellsClient getCellsClient(CellsTransport transport) {
        return new CellsClient(transport, new S3Client(transport));
    }

    @Override
    public CustomEncoder getEncoder() {
        return new JavaCustomEncoder();
    }

}