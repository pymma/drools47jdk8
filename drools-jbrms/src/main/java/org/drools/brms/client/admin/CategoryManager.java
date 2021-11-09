package org.drools.brms.client.admin;
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



import org.drools.brms.client.categorynav.CategoryEditor;
import org.drools.brms.client.categorynav.CategoryExplorerWidget;
import org.drools.brms.client.categorynav.CategorySelectHandler;
import org.drools.brms.client.common.FormStyleLayout;
import org.drools.brms.client.common.GenericCallback;
import org.drools.brms.client.common.ImageButton;
import org.drools.brms.client.rpc.RepositoryServiceFactory;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This controls category administration.
 * @author Michael Neale
 */
public class CategoryManager extends Composite {

    public VerticalPanel layout = new VerticalPanel();
    //public String selectedPath;
    private CategoryExplorerWidget explorer;
    
    public CategoryManager() {
        
        FormStyleLayout form = new FormStyleLayout("images/edit_category.gif", "Edit categories");
        form.addAttribute( "", new HTML("<i>Categories aid in managing large numbers of rules/assets. A shallow hierarchy is recommented.</i>") );

        explorer = new CategoryExplorerWidget(new CategorySelectHandler() {
            public void selected(String sel) {
                //don't need this here as we don't do anything on select in this spot
            }
         });
        explorer.setStyleName( "category-explorer-Admin" );
        
        SimplePanel editable = new SimplePanel();
        editable.setStyleName( "metadata-Widget" );
        editable.add( explorer );
        form.addRow( new HTML("<hr/>") );
        form.addAttribute( "Current categories:", editable );
        
        Image refresh = new ImageButton( "images/refresh.gif" );
        refresh.setTitle( "Refresh categories" );
        refresh.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                explorer.refresh();
            }
        } );
        form.addAttribute( "Refresh view:", refresh );
        form.addRow( new HTML("<hr/>") );
        
        Image newCat = new ImageButton( "images/new.gif" );
        newCat.setTitle( "Create a new category" );
        newCat.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                CategoryEditor newCat = new CategoryEditor( explorer.getSelectedPath() );
                newCat.setPopupPosition( w.getAbsoluteLeft(),
                                         w.getAbsoluteTop() - 400 );
                newCat.show();
            }
        } );
        
        form.addAttribute( "Create a new category:", newCat );
        
        Image delete = new ImageButton("images/delete_obj.gif");
        delete.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                deleteSelected();
            }
        } );
        delete.setTitle( "Deletes the currently selected category. You won't be able to delete if the category is in use." );
        
        form.addAttribute( "Delete the currently selected category:", delete );
        
        initWidget( form );
    }
    
    
    private void deleteSelected() {
        if (Window.confirm( "Are you sure you want to delete category: " + explorer.getSelectedPath() )) {
            RepositoryServiceFactory.getService().removeCategory( explorer.getSelectedPath(), new GenericCallback() {

                public void onSuccess(Object data) {
                    explorer.refresh();
                }
                
            });
        }        
    }


    
}