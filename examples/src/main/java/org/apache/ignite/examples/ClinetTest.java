package org.apache.ignite.examples;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Created by GridGain on 03.06.2015.
 */
public class ClinetTest {
    private static final String HOST = "127.0.0.1";

    /** Http port. */
    private static final int HTTP_PORT = 9090;

    /** Url address to send HTTP request. */
    private static final String TEST_URL = "http://" + HOST + ":" + HTTP_PORT + "/ignite";


    /** Used to sent request charset. */
    private static final String CHARSET = StandardCharsets.UTF_8.name();


    public static void main(String[] args) throws Exception {
            String qry = "cmd=version";

            URLConnection connection = new URL(TEST_URL + "?" + qry).openConnection();

            connection.setRequestProperty("Accept-Charset", CHARSET);

            BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String res = r.readLine();

            r.close();

    }
}
