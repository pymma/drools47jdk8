package org.drools.brms.client.ruleeditor;
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



import org.drools.brms.client.categorynav.CategoryExplorerWidget;
import org.drools.brms.client.categorynav.CategorySelectHandler;
import org.drools.brms.client.common.DirtyableComposite;
import org.drools.brms.client.common.DirtyableFlexTable;
import org.drools.brms.client.common.ImageButton;
import org.drools.brms.client.rpc.MetaData;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This is a viewer/editor for categories.
 * It will show a list of categories currently applicable, and allow you to 
 * remove/add to them.
 * 
 * It is intended to work with the meta data form.
 * 
 * @author Michael Neale
 * @author Fernando Meyer
 */
public class AssetCategoryEditor extends DirtyableComposite {

    private MetaData data;
    private DirtyableFlexTable layout = new DirtyableFlexTable();
    private FlexTable list;
    
    
    /**
     * @param d The meta data.
     * @param readOnly If it is to be non editable.
     * @param change This will be called when a change is made (data in MetaData will be changed).
     */
    public AssetCategoryEditor(MetaData d, boolean readOnly) {
        this.data = d;

        list = new FlexTable();

        loadData( list );  
        list.setStyleName( "rule-List" );
        layout.setWidget( 0, 0, list );
        
        if (!readOnly) {
            doActions();
        }
        
        initWidget( layout );        
    }

    private void doActions() {
        VerticalPanel actions = new VerticalPanel();
        Image add = new ImageButton("images/new_item.gif");
        add.setTitle( "Add a new category." );
        
        add.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                doOKClick();
            }            
        });
        
        
        actions.add( add );
        layout.setWidget( 0, 1, actions );
        
    }

    protected void removeCategory(int idx) {
        data.removeCategory( idx );
        makeDirty();
        resetBox();
    }

    private void resetBox() {
        list = new FlexTable();
        list.setStyleName( "rule-List" );
        layout.setWidget( 0, 0, list );        
        loadData( list );
        makeDirty();
    }

    private void loadData(FlexTable list) {
        for ( int i = 0; i < data.categories.length; i++ ) {
            final int idx = i;
            
            list.setText( i, 0, data.categories[i] );
            Image del = new ImageButton("images/trash.gif");
            del.setTitle( "Remove this category" );
            del.addClickListener( new ClickListener() {
                public void onClick(Widget w) {
                    removeCategory(idx);
                }
            } );
            list.setWidget( i, 1, del );            
        }
    }
    
    


    /** Handles the OK click on the selector popup */
    private void doOKClick() {

        CategorySelector sel = new CategorySelector();
        sel.setPopupPosition( getAbsoluteLeft(), getAbsoluteTop() );
        sel.show();  
    }




    /**
     * Appy the change (selected path to be added).
     */
    public void addToCategory(String selectedPath) {

        
        data.addCategory( selectedPath );
        resetBox();
    }





    /**
     * This is a popup that allows you to select a category to add to the asset.
     */
    class CategorySelector extends PopupPanel {
        
        public Button ok = new Button("OK");
        private CategoryExplorerWidget selector;  
        public String selectedPath;
       
        public CategorySelector() {
            super(true);
            VerticalPanel vert = new VerticalPanel();
        
            selector = new CategoryExplorerWidget(new CategorySelectHandler() {
                public void selected(String sel) {
                    selectedPath = sel;
                }
                
            });
            
            this.setStyleName( "ks-popups-Popup" );
            
            vert.add( selector );
            vert.add( ok );
            
            add( vert );
            
            ok.addClickListener( new ClickListener() {
                public void onClick(Widget w) { 
                    if (selectedPath != null &&  !"".equals(selectedPath)) {
                        addToCategory(selectedPath);
                    }
                    hide();
                }            
            });
          
        }      

    }
}