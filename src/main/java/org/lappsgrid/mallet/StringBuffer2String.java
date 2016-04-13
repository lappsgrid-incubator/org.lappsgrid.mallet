package org.lappsgrid.mallet;


import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class StringBuffer2String extends Pipe implements Serializable {

    public Instance pipe (Instance carrier) {

        if (carrier.getData() instanceof StringBuffer) {
            StringBuffer data = (StringBuffer) carrier.getData();
            carrier.setData(data.toString());
        }
        else {
            throw new IllegalArgumentException("StringBuffer2String expects a StringBuffer, found a " + carrier.getData().getClass());
        }

        return carrier;
    }

    // Serialization

    private static final long serialVersionUID = 1;
    private static final int CURRENT_SERIAL_VERSION = 0;

    private void writeObject (ObjectOutputStream out) throws IOException {
        out.writeInt (CURRENT_SERIAL_VERSION);
    }

    private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt ();
    }

}