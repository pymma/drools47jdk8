package org.drools.brms.client.decisiontable;
/*
 * Copyright 2005 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import org.drools.brms.client.decisiontable.EditableDTGrid.RowClickListener;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

/**
 * This shows the widget for merging cells in a row.
 * @author Steven Williams
 */
public class MergeAction extends Composite {

    private HorizontalPanel panel = new HorizontalPanel();
    private Image merge;
    private int row;
    

    
    
    /**
     * Pass in the click listener delegate for when the respective action is clicked
     * @param clickListener
     * @param okClickListener
     */
    public MergeAction(final int currentRow, final RowClickListener clickListener) {
        row = currentRow;
        merge = new Image("images/refresh.gif");
        merge.setTitle( "Merge cells in this row" );
        merge.addClickListener( new ClickListener() {
			public void onClick(final Widget w) {
				clickListener.onClick(w, row);
			}
        });
        
        panel.add( merge);
        
        initWidget( panel );
    }
    
    public void setRow(final int row) {
    	this.row = row;
    }
    
    public int getRow() {
    	return row;
    }
    
}