package com.wherehoo.final2001;

import java.io.IOException;
import java.net.Socket;

abstract class WHOperation{
 
    abstract void executeAndOutputToClient(Socket client_socket) throws IOException ;

    abstract void setVerbose(int level);
 }
