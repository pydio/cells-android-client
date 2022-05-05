package com.pydio.android.cells.test;

import com.pydio.cells.api.callbacks.NodeHandler;
import com.pydio.cells.api.ui.Node;
import com.pydio.cells.utils.Log;

public class DebugNodeHandler implements NodeHandler {

    private final String caller;

    public DebugNodeHandler(String caller) {
        this.caller = caller;
    }

    private int i = 0;

    @Override
    public void onNode(Node node) {
        Log.i(caller, "#" + (++i) + " " + node.getName());
    }
}