package org.mortbay.jetty.ajp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.io.Buffer;
import org.mortbay.io.EndPoint;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpException;
import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.RetryRequest;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.EofException;
import org.mortbay.jetty.HttpStatus;
import org.mortbay.log.Log;
import org.mortbay.util.URIUtil;
import org.mortbay.util.ajax.Continuation;

/**
 * Connection implementation of the Ajp13 protocol. <p/> XXX Refactor to remove
 * duplication of HttpConnection
 * 
 * @author Markus Kobler
 */
public class Ajp13Connection extends HttpConnection
{
    private boolean _sslSecure=false;

    public Ajp13Connection(Connector connector, EndPoint endPoint, Server server, int bufferSize)
    {
        super(connector,endPoint,server);
        
        // TODO avoid the creation of HttpParsers etc.
        
        _parser=new Ajp13Parser(_connector,_endp,new RequestHandler(),bufferSize);
        // _generator=new Ajp13Generator(_connector,_endp,bufferSize);
    }

    public boolean isConfidential(Request request)
    {
        return _sslSecure;
    }

    public boolean isIntegral(Request request)
    {
        return _sslSecure;
    }

    public ServletInputStream getInputStream()
    {
        if (_in==null)
            _in=new Ajp13Parser.Input((Ajp13Parser)_parser,_connector.getMaxIdleTime());
        return _in;
    }

    // XXX Implement
    public ServletOutputStream getOutputStream()
    {
        return new ServletOutputStream()
        {
            public void write(int b) throws IOException
            {
                System.out.write(b);
            }
        };
    }

    // XXX Implement
    public PrintWriter getPrintWriter(String encoding)
    {
        return new PrintWriter(getOutputStream());
    }

    
    private class RequestHandler implements Ajp13Parser.EventHandler
    {
        public void startForwardRequest() throws IOException
        {
            // TODO - note that I tend to use println instead of debug for stuff that you do
            // not want to stay in the code long term.
            
            System.err.println("AJP13 START");
            _uri.clear();
            _sslSecure=false;
            _request.setTimeStamp(System.currentTimeMillis());
            _request.setUri(_uri);
        }

        public void parsedMethod(Buffer method) throws IOException
        {
            Log.debug("AJP13 METHOD '{}'",method);

            _request.setMethod(method.toString());
        }

        public void parsedUri(Buffer uri) throws IOException
        {
            System.err.println("AJP13 URI "+uri);

            _uri.parse(uri.array(), uri.getIndex(), uri.length());
        }

        public void parsedProtocol(Buffer protocol) throws IOException
        {
            System.err.println("AJP13 PROTOCOL "+protocol);

            _request.setProtocol(protocol.toString());
        }

        public void parsedRemoteAddr(Buffer addr) throws IOException
        {
            System.err.println("AJP13 REMOTE ADDR "+addr);

            // XXX Is the remote address used anywhere?
        }

        public void parsedRemoteHost(Buffer name) throws IOException
        {
            System.err.println("AJP13 REMOTE HOST "+name);

            // XXX Is the remote host used anywhere?
        }

        public void parsedServerName(Buffer name) throws IOException
        {
            System.err.println("AJP13 SERVER NAME "+name);

            // TODO So long as we get Host header, this is probably not needed?
            // but probably should remember as default if no header available
            // _uri.setHost(name.toString());
        }

        public void parsedServerPort(int port) throws IOException
        {
            System.err.println("AJP13 SERVER PORT "+new Integer(port));

            // TODO So long as we get Host header, this is probably not needed?
            // but probably should remember as default if no header available
            // _uri.setPort(port);
        }

        public void parsedSslSecure(boolean secure) throws IOException
        {
            System.err.println("AJP13 SSL SECURE "+secure);

            _sslSecure=secure;
        }

        public void parsedQueryString(Buffer value) throws IOException
        {
            System.err.println("AJP13 QUERY STRING "+value);

            String u=_uri+"?"+value;
            _uri.parse(u);
        }

        public void parsedHeader(Buffer name, Buffer value) throws IOException
        {
            System.err.println("AJP13 Header "+name+" = "+value);

            _requestFields.add(name,value);
        }

        public void parsedRequestAttribute(String key, Buffer value) throws IOException
        {
            System.err.println("AJP13 Attr "+key+" = "+value);

            _request.setAttribute(key,value.toString());
        }

        public void headerComplete() throws IOException
        {
            System.err.println("AJP13 Header Complete");
            
            handleRequest();
        }

    }

}
