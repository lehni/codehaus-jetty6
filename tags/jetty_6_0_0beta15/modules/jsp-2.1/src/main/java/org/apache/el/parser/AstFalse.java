/* Generated By:JJTree: Do not edit this line. AstFalse.java */

package org.apache.el.parser;

import javax.el.ELException;

import org.apache.el.lang.EvaluationContext;


/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: dpatil $
 */
public final class AstFalse extends BooleanNode {
    public AstFalse(int id) {
        super(id);
    }

    public Object getValue(EvaluationContext ctx)
            throws ELException {
        return Boolean.FALSE;
    }
}
