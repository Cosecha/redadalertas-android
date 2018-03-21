package com.redadalertas.client;

import android.util.Log;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.io.AbstractSessionInputBuffer;
import org.apache.http.impl.io.HttpResponseParser;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicLineParser;
import org.apache.http.params.BasicHttpParams;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Mostly copied from https://stackoverflow.com/questions/9880117/how-to-convert-a-string-to-an-apache-httpcomponents-httprequest/10581901#10581901
 * Thanks internet!
 *
 * Important Note: only supports responses in UTF-8. Period. Sorry not sorry.
 */
public class ApacheResponseFactory {
    private static final String charset = "utf-8";
    public static HttpResponse parse(final String entireResponse) {
        try {
            SessionInputBuffer inputBuffer = new AbstractSessionInputBuffer() {
                {
                    init(new ByteArrayInputStream(entireResponse.getBytes()), 10, new BasicHttpParams());
                }

                @Override
                public boolean isDataAvailable(int timeout) throws IOException {
                    throw new RuntimeException("have to override but probably not even called");
                }
            };
            HttpMessageParser parser = new HttpResponseParser(inputBuffer, new BasicLineParser(new ProtocolVersion("HTTP", 1, 1)), new DefaultHttpResponseFactory(), new BasicHttpParams());
            HttpResponse message = (HttpResponse)parser.parse();
            String messageParts[] = entireResponse.split("\r\n\r\n"); // this is the only manual HTTP parsing I will consent to
            if (messageParts.length != 2) {
                Log.e("ApacheResponseFactory", "this is some kind of messed up HTTP message, boo!");
                Log.e("ApacheResponseFactory", "Lines: " + messageParts.length);
                Log.e("ApacheResponseFactory", entireResponse);
                throw new ParseException();
            }
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new ByteArrayInputStream(messageParts[1].getBytes(charset)));
            message.setEntity(entity);
            return message;
        } catch (HttpException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
