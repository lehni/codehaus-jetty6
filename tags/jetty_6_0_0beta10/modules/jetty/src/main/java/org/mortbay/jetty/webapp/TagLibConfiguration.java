//========================================================================
//$Id: TagLibConfiguration.java,v 1.4 2005/08/13 00:01:27 gregwilkins Exp $
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty.webapp;

import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.mortbay.jetty.webapp.Configuration;
import org.mortbay.log.Log;
import org.mortbay.resource.Resource;
import org.mortbay.util.LazyList;
import org.mortbay.xml.XmlParser;

/* ------------------------------------------------------------ */
/** TagLibConfiguration.
 * 
 * The class searches for TLD descriptors found in web.xml, in WEB-INF/*.tld files of the web app
 * or *.tld files withing jars found in WEB-INF/lib of the webapp.   Any listeners defined in these
 * tld's are added to the context.
 * 
 * &lt;bile&gt;This is total rubbish special case for JSPs! If there was a general use-case for web app
 * frameworks to register listeners directly, then a generic mechanism could have been added to the servlet
 * spec.  Instead some special purpose JSP support is required that breaks all sorts of encapsualtion rules as
 * the servlet container must go searching for and then parsing the descriptors for one particular framework.
 * It only appears to be used by JSF, which is being developed by the same developer who implemented this
 * feature in the first place!
 * &lt;/bile&gt;
 * 
 * @author gregw
 *
 */
public class TagLibConfiguration implements Configuration
{
    WebAppContext _context;
    
    public void setWebAppContext(WebAppContext context)
    {
        _context=context;
    }

    public WebAppContext getWebAppContext()
    {
        return _context;
    }

    public void configureClassLoader() throws Exception
    {
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.servlet.WebAppContext.Configuration#configureDefaults()
     */
    public void configureDefaults() throws Exception
    {
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.servlet.WebAppContext.Configuration#configureWebApp()
     */
    public void configureWebApp() throws Exception
    {
        Set tlds = new HashSet();
        
        // Find tld's from web.xml
        // When the XMLConfigurator (or other configurator) parsed the web.xml,
        // It should have created aliases for all TLDs.  So search resources aliases
        // for aliases ending in tld
        if (_context.getResourceAliases()!=null)
        {
            Iterator iter=_context.getResourceAliases().values().iterator();
            while(iter.hasNext())
            {
                String location = (String)iter.next();
                if (location!=null && location.toLowerCase().endsWith(".tld"))
                {
                    if (!location.startsWith("/"))
                        location="/WEB-INF/"+location;
                    Resource l=_context.getBaseResource().addPath(location);
                    tlds.add(l);
                }
            }
        }
        
        // Look for any tlds in WEB-INF directly.
        if (_context.getWebInf()!=null)
        {
            String[] contents = _context.getWebInf().list();
            for (int i=0;i<contents.length;i++)
            {
                if (contents[i]!=null && contents[i].toLowerCase().endsWith(".tld"))
                {
                    Resource l=_context.getWebInf().addPath(contents[i]);
                    tlds.add(l);
                }
                
            }

            // Look for any tlds in the META-INF of included jars
            Resource lib=_context.getWebInf().addPath("lib/");
            if (lib.exists() && lib.isDirectory())
            {
                contents = lib.list();
                for (int i=0;i<contents.length;i++)
                {
                    if (contents[i]!=null && contents[i].toLowerCase().endsWith(".jar"))
                    {
                        Resource l=lib.addPath(contents[i]);
                        Resource meta=Resource.newResource("jar:"+l+"!/META-INF/");
                        if (meta.exists())
                        {
                            String[] meta_contents=meta.list();
                            
                            for (int j=0;j<meta_contents.length;j++)
                            {
                                if (meta_contents[j]!=null && meta_contents[j].toLowerCase().endsWith(".tld"))
                                {
                                    Resource t=meta.addPath(meta_contents[j]);
                                    tlds.add(t);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Create a TLD parser
        XmlParser parser = new XmlParser(false);
        
        parser.redirectEntity("web-jsptaglibrary_1_1.dtd",WebAppContext.class.getResource("/javax/servlet/jsp/web-jsptaglibrary_1_1.dtd"));
        parser.redirectEntity("web-jsptaglibrary_1_2.dtd",WebAppContext.class.getResource("/javax/servlet/jsp/web-jsptaglibrary_1_2.dtd"));
        parser.redirectEntity("web-jsptaglibrary_2_0.xsd",WebAppContext.class.getResource("/javax/servlet/jsp/web-jsptaglibrary_2_0.xsd"));
        parser.setXpath("/taglib/listener/listener-class");
        // Parse all the discovered TLDs
        Iterator iter = tlds.iterator();
        while (iter.hasNext())
        {
            try
            {
                Resource tld = (Resource)iter.next();
                if (Log.isDebugEnabled()) Log.debug("TLD="+tld);
                
                XmlParser.Node root = parser.parse(tld.getURL().toString());
                
                for (int i=0;i<root.size();i++)
                {
                    Object o=root.get(i);
                    if (o instanceof XmlParser.Node)
                    {
                        XmlParser.Node node = (XmlParser.Node)o;
                        if ("listener".equals(node.getTag()))
                        {
                            String className=node.getString("listener-class",false,true);
                            if (Log.isDebugEnabled()) Log.debug("listener="+className);
                            
                            try
                            {
                                Class listenerClass=getWebAppContext().loadClass(className);
                                EventListener l=(EventListener)listenerClass.newInstance();
                                _context.addEventListener(l);
                            }
                            catch(Exception e)
                            {
                                Log.warn("Could not instantiate listener "+className,e);
                            }
                        }
                    }
                }
            }
            catch(Exception e)
            {
                Log.warn(e);
            }
        }
    }


    public void deconfigureWebApp() throws Exception
    {
    }
}
