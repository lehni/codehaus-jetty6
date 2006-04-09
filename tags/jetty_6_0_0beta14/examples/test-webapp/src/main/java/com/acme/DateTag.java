package com.acme;

import java.text.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import java.io.*;
import java.util.*;

public class DateTag extends BodyTagSupport
{
    Tag parent;
    BodyContent body;
    String tz="GMT";

    public void setParent(Tag parent) {this.parent=parent;}
    public Tag getParent() {return parent;}
    public void setBodyContent(BodyContent content) {body=content;}
    public void setPageContext(PageContext pageContext) {}

    public void setTz(String value) {tz=value;}

    public int doStartTag() throws JspException {return EVAL_BODY_TAG;}
    
    public int doEndTag() throws JspException {return EVAL_PAGE;}

    public void doInitBody() throws JspException {}

    public int doAfterBody() throws JspException {
	try
	{
            SimpleDateFormat format = new SimpleDateFormat(body.getString());
            format.setTimeZone(TimeZone.getTimeZone(tz));
	    body.getEnclosingWriter().write(format.format(new Date()));
	    return SKIP_BODY;
	}
	catch (Exception ex) {
            ex.printStackTrace();
            throw new JspTagException(ex.toString());
	}
    }

    public void release()
    {
	body=null;
    }
}

