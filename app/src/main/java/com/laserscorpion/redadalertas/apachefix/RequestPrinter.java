package com.laserscorpion.redadalertas.apachefix;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;


public class RequestPrinter {
    private static final String APP_NAME = "com.laserscorpion.redadalertas";
    private static final String CRLF = "\r\n";
    private String version = "*";

    public RequestPrinter(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(APP_NAME, 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("RequestPrinter","Did we change the package name?");
            e.printStackTrace();
        }
    }

    public String print(HttpRequest request) {
        String requestLine = printRequestLine(request);
        String headers[] = printHeaders(request);
        String result = requestLine;
        for (String header : headers) {
            result += header;
        }
        result += CRLF;
        return result;
    }

    private String printRequestLine(HttpRequest request) {
        RequestLine line = request.getRequestLine();
        String result = line.getMethod() + " ";
        result += line.getUri() + " ";
        result += line.getProtocolVersion() + CRLF;
        return result;
    }

    private String[] printHeaders(HttpRequest request) {
        Header headers[] = request.getAllHeaders();
        String headerStrings[] = new String[headers.length + 1];
        for (int i = 0; i < headers.length; i++) {
            headerStrings[i] = headers[i].getName();
            headerStrings[i] += ": ";
            headerStrings[i] += headers[i].getValue();
            headerStrings[i] += CRLF;
        }
        headerStrings[headers.length] = "User-Agent: RedadAlertas Android " + version;
        return headerStrings;
    }
}
