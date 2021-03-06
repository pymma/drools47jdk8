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



import java.util.List;

import org.drools.brms.client.common.DirtyableComposite;
import org.drools.brms.client.common.FieldEditListener;
import org.drools.brms.client.common.FormStylePopup;
import org.drools.brms.client.common.InfoPopup;
import org.drools.brms.client.common.Lbl;
import org.drools.brms.client.modeldriven.SuggestionCompletionEngine;
import org.drools.brms.client.modeldriven.brl.FactPattern;
import org.drools.brms.client.modeldriven.brl.ISingleFieldConstraint;
import org.drools.brms.client.modeldriven.brl.RuleModel;
import org.drools.brms.client.modeldriven.brl.SingleFieldConstraint;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * This is an editor for constraint values.
 * How this behaves depends on the constraint value type.
 * When the constraint value has no type, it will allow the user to choose the first time.
 *
 * @author Michael Neale
 * @author Fernando Meyer
 */
public class ConstraintValueEditor extends DirtyableComposite {

    private final ISingleFieldConstraint constraint;
    private final Panel      panel;
    private final RuleModel model;
    private final boolean numericValue;
    private String[] enumeratedValues;

    /**
     * @param con The constraint being edited.
     */
    public ConstraintValueEditor(FactPattern pattern, String fieldName, ISingleFieldConstraint con, RuleModeller modeller, String valueType /* eg is numeric */) {
        this.constraint = con;
        if (valueType.equals( SuggestionCompletionEngine.TYPE_NUMERIC )) {
            this.numericValue = true;
        } else {
            this.numericValue = false;
        }
        if (valueType.equals( SuggestionCompletionEngine.TYPE_BOOLEAN )) {
            this.enumeratedValues = new String[] {"true", "false" };
        }
        this.model = modeller.getModel();
        SuggestionCompletionEngine sce = modeller.getSuggestionCompletions();
        this.enumeratedValues = sce.getEnums(pattern, fieldName);


        panel = new SimplePanel();
        refreshEditor();
        initWidget( panel );

    }

    private void refreshEditor() {
        panel.clear();

        if ( constraint.constraintValueType == SingleFieldConstraint.TYPE_UNDEFINED ) {
            Image clickme = new Image( "images/edit.gif" );
            clickme.addClickListener( new ClickListener() {
                public void onClick(Widget w) {
                    showTypeChoice( w, constraint );
                }
            } );
            panel.add( clickme );
        } else {
            switch ( constraint.constraintValueType ) {
                case SingleFieldConstraint.TYPE_LITERAL :
                    panel.add( literalEditor() );
                    break;
                case SingleFieldConstraint.TYPE_RET_VALUE :
                    panel.add( returnValueEditor() );
                    break;
                case SingleFieldConstraint.TYPE_VARIABLE :
                    panel.add( variableEditor() );
                    break;
                default :
                    break;
            }
        }
    }

    private Widget variableEditor() {
        List vars = this.model.getBoundVariablesInScope( this.constraint );

        final ListBox box = new ListBox();

        if (this.constraint.value == null) {
            box.addItem( "Choose ..." );
        }
        for ( int i = 0; i < vars.size(); i++ ) {
            String var = (String) vars.get( i );
            box.addItem( var );
            if (this.constraint.value != null && this.constraint.value.equals( var )) {
                box.setSelectedIndex( i );
            }
        }

        box.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                constraint.value = box.getItemText( box.getSelectedIndex() );
            }
        });

        return box;
    }

    /**
     * An editor for the retval "formula" (expression).
     */
    private Widget returnValueEditor() {
        TextBox box = boundTextBox(constraint);
        String msg = "This is a formula expression which will evaluate to a value.";
        Image img = new Image("images/function_assets.gif");
        img.setTitle( msg );
        box.setTitle( msg );
        Widget ed = widgets( img, box);
        return ed;
    }

    /**
     * An editor for literal values.
     */
    private Widget literalEditor() {

        //use a drop down if we have a fixed list
        if (this.enumeratedValues != null) {
            return enumDropDown(constraint.value, new ValueChanged() {
                public void valueChanged(String newValue) {
                    constraint.value = newValue;
                }
            }, this.enumeratedValues);
        } else {

            final TextBox box = boundTextBox(constraint);

            if (this.numericValue) {
                box.addKeyboardListener( new KeyboardListener() {

                    public void onKeyDown(Widget arg0, char arg1, int arg2) {

                    }

                    public void onKeyPress(Widget w, char c, int i) {
                        if (Character.isLetter( c ) ) {
                            ((TextBox) w).cancelKey();
                        }
                    }

                    public void onKeyUp(Widget arg0, char arg1, int arg2) {
                    }

                } );
            }

            box.setTitle( "This is a literal value. What is shown is what the field is checked against." );
            return box;
        }
    }

    /**
     * This will do a drop down for enumerated values..
     */
    public static Widget enumDropDown(//final ISingleFieldConstraint constraint,
                                       final String currentValue, final ValueChanged valueChanged,
                                final String[] enumeratedValues) {
        final ListBox box = new ListBox();

        if (currentValue == null || "".equals( currentValue )) {
            box.addItem( "Choose ..." );
        }

        boolean selected = false;

        for ( int i = 0; i < enumeratedValues.length; i++ ) {
            String v = enumeratedValues[i];
            String val;
            if (v.indexOf( '=' ) > 0) {
                //using a mapping
                String[] splut = splitValue(v);
                String realValue = splut[0];
                String display = splut[1];
                val = realValue;
                box.addItem( display, realValue );
            } else {
                box.addItem( v, v );
                val = v;
            }
            if (currentValue != null && currentValue.equals( val )) {
                box.setSelectedIndex( i );
                selected = true;
            }
        }

        if (currentValue != null && !selected) {
            //need to add this value
            box.addItem( currentValue, currentValue );
            box.setSelectedIndex( enumeratedValues.length );
        }

        box.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                valueChanged.valueChanged( box.getValue( box.getSelectedIndex() ) );
                //constraint.value = box.getValue( box.getSelectedIndex() );
            }
        });
        return box;
    }

    /**
     * 'Person.age' : ['M=Male', 'F=Female']
     *
     * This will split the drop down item into a value and a key.
     * eg key=value
     *
     */
    public static String[] splitValue(String v) {
        String[] s = new String[2];
        int pos = v.indexOf( '=' );
        s[0] = v.substring( 0, pos );
        s[1] = v.substring( pos + 1, v.length() );
        return s;
    }

    private TextBox boundTextBox(final ISingleFieldConstraint c) {
        final TextBox box = new TextBox();
        box.setStyleName( "constraint-value-Editor" );
        if (c.value == null) {
        	box.setText("");
        } else {
        	box.setText( c.value );
        }


        if (c.value == null || c.value.length() < 5 ) {
            box.setVisibleLength( 3 );
        } else {
            box.setVisibleLength( c.value.length() - 1 );
        }


        box.addChangeListener( new ChangeListener() {
            public void onChange(Widget w) {
                c.value = box.getText();
                makeDirty();
            }

        } );

        box.addKeyboardListener( new FieldEditListener( new Command() {
            public void execute() {
                box.setVisibleLength( box.getText().length() );
            }
        } ) );



        return box;
    }

    /**
     * Show a list of possibilities for the value type.
     */
    private void showTypeChoice(Widget w, final ISingleFieldConstraint con) {
        final FormStylePopup form = new FormStylePopup( "images/newex_wiz.gif",
                                                        "Field value" );

        Button lit = new Button( "Literal value" );
        lit.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                con.constraintValueType = SingleFieldConstraint.TYPE_LITERAL;
                doTypeChosen( form );
            }

        } );
        form.addAttribute( "Literal value:", widgets( lit, new InfoPopup( "Literal",
                                                                          "A literal value means the " + "constraint is directly against the value that you type (ie. what you see on screen)." ) ) );




        form.addRow( new HTML( "<hr/>" ) );
        form.addRow( new Lbl( "Advanced options",
                              "weak-Text" ) );

        //only want to show variables if we have some !
        if (this.model.getBoundVariablesInScope( this.constraint ).size() > 0) {
            Button variable = new Button("Bound variable");
            variable.addClickListener( new ClickListener() {
                public void onClick(Widget w) {
                    con.constraintValueType = SingleFieldConstraint.TYPE_VARIABLE;
                    doTypeChosen( form );
                }
            });
            form.addAttribute( "A variable:", widgets( variable, new InfoPopup("A bound variable", "Will apply a constraint that compares a field to a bound variable.")) );
        }

        Button formula = new Button( "New formula" );
        formula.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                con.constraintValueType = SingleFieldConstraint.TYPE_RET_VALUE;
                doTypeChosen( form );
            }
        } );

        form.addAttribute( "A formula:", widgets( formula, new InfoPopup( "A formula",
                                                                          "A formula is an expression that calculates and returns a value " + ". That value is used to enforce the constraint." ) ) );

        form.setPopupPosition( w.getAbsoluteLeft(), w.getAbsoluteTop() );
        form.show();
    }

    private void doTypeChosen(final FormStylePopup form) {
        refreshEditor();
        form.hide();
    }

    private Panel widgets(Widget left, Widget right) {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add( left );
        panel.add( right );
        panel.setWidth("100%");
        return panel;
    }

    public boolean isDirty() {
        return super.isDirty();
    }


    static interface ValueChanged {
        public void valueChanged(String newValue);
    }


}