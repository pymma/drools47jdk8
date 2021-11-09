package org.drools.brms.client.modeldriven.ui;
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



import org.drools.brms.client.common.DirtyableComposite;
import org.drools.brms.client.common.DirtyableHorizontalPane;
import org.drools.brms.client.common.FormStyleLayout;
import org.drools.brms.client.common.YesNoDialog;
import org.drools.brms.client.modeldriven.brl.RuleAttribute;
import org.drools.brms.client.modeldriven.brl.RuleModel;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a list of rule options (attributes).
 * 
 * @author Michael Neale
 */
public class RuleAttributeWidget extends DirtyableComposite {

    private FormStyleLayout layout;
    private RuleModel model;
    private RuleModeller parent;

    
    public RuleAttributeWidget(RuleModeller parent, RuleModel model) {
        this.parent = parent;
        this.model = model;
        layout = new FormStyleLayout();
        RuleAttribute[] attrs = model.attributes;
        for ( int i = 0; i < attrs.length; i++ ) {
            RuleAttribute at = attrs[i];
            layout.addAttribute( at.attributeName, getEditorWidget(at, i));
        }
        
        initWidget( layout );
    }

    /**
     * Return a listbox of choices for rule attributes.
     * @return
     */
    public static ListBox getAttributeList() {
        ListBox list = new ListBox();
        list.addItem( "Choose..." );
        
        list.addItem( "salience" );
        list.addItem( "enabled" );
        list.addItem( "date-effective" );
        list.addItem( "date-expires" );
        list.addItem( "no-loop" );
        list.addItem( "agenda-group" );
        list.addItem( "activation-group" );
        list.addItem( "duration" );
        list.addItem( "auto-focus" ); 
        list.addItem( "lock-on-active" );
        list.addItem( "ruleflow-group" );
        list.addItem( "dialect" );
        
        return list;
    }

    private Widget getEditorWidget(final RuleAttribute at, final int idx) {
        if (at.attributeName.equals( "no-loop" )) {
            return getRemoveIcon( idx );
        }
        
        Widget editor = null;
        
        if (at.attributeName.equals( "enabled" ) 
                || at.attributeName.equals( "auto-focus" )
                || at.attributeName.equals( "lock-on-active" )) {
            editor = checkBoxEditor( at );
        } else {
            editor = textBoxEditor( at );
        }
        
        
        DirtyableHorizontalPane horiz = new DirtyableHorizontalPane();
        horiz.add( editor );
        horiz.add( getRemoveIcon( idx ) );        
        
        return horiz;
        
    }

    private Widget checkBoxEditor(final RuleAttribute at) {
        final CheckBox box = new CheckBox();
        if (at.value == null) {
            box.setChecked( true );
            at.value = "true";
        } else {
            box.setChecked( ( at.value.equals( "true" ) ? true : false) );  
        }
        
        
        box.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                at.value = (box.isChecked()) ? "true" : "false";
            }
        });
        return box;
    }

    private TextBox textBoxEditor(final RuleAttribute at) {
        final TextBox box = new TextBox();
        box.setVisibleLength( (at.value.length() < 3) ? 3 : at.value.length() );
        box.setText( at.value );
        box.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                at.value = box.getText();
                makeDirty();
            }
        });
        
        if (at.attributeName.equals( "date-effective" ) || at.attributeName.equals( "date-expires" )) {
            if (at.value == null || "".equals( at.value )) box.setText( "dd-MMM-yyyy" );
                
            box.setVisibleLength( 10 );
        }
        

        
        box.addKeyboardListener( new KeyboardListener() {

            public void onKeyDown(Widget arg0,
                                  char arg1,
                                  int arg2) {

                
            }

            public void onKeyPress(Widget arg0,
                                   char arg1,
                                   int arg2) {
                
            }

            public void onKeyUp(Widget arg0,
                                char arg1,
                                int arg2) {
                box.setVisibleLength( box.getText().length() );
            }
            
        });
        return box;
    }

    private Image getRemoveIcon(final int idx) {
        Image remove = new Image( "images/delete_item_small.gif" );
        remove.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                YesNoDialog diag = new YesNoDialog("Remove this rule option?", new Command() {
                    public void execute() {
                        model.removeAttribute( idx);
                        parent.refreshWidget();                            
                    }
                });
                diag.setPopupPosition( w.getAbsoluteLeft(), w.getAbsoluteTop() );
                diag.show();
            }
        } );
        return remove;
    }
    
    public boolean isDirty() {
        return layout.isDirty();
    }
       
}