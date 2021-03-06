/**
 * 
 */
package org.drools.reteoo;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.graphics.Color;

/**
 * Wraps {@link NotNode} and adds visual extras like color information
 *
 */
public class NotNodeVertex extends BaseVertex {
    
    private static final String NODE_NAME = "NotNode";
    
    private final NotNode node;

    /**
     * Constructor
     * 
     * @param node node to be wrapped
     */
    public NotNodeVertex(final NotNode node) {
        super();
        this.node = node;
    }

    /* (non-Javadoc)
     * @see org.drools.reteoo.BaseNodeVertex#getHtml()
     */
    public String getHtml() {
        return NODE_NAME+" : " + this.node.getId();
    }

    /* (non-Javadoc)
     * @see org.drools.eclipse.editors.rete.model.BaseVertex#toString()
     */
    public String toString() {
        return NODE_NAME;
    }

    /* (non-Javadoc)
     * @see org.drools.reteoo.BaseNodeVertex#getFillColor()
     */
    public Color getFillColor() {
        return ColorConstants.cyan;
    }

    /**
     * Node ID
     * 
     * @return node id
     */
    public int getId() {
        return node.getId();
    }

}