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



import org.drools.brms.client.common.DirtyableComposite;
import org.drools.brms.client.common.DirtyableFlexTable;
import org.drools.brms.client.common.FormStylePopup;
import org.drools.brms.client.common.GenericCallback;
import org.drools.brms.client.common.LoadingPopup;
import org.drools.brms.client.packages.PackageBuilderWidget;
import org.drools.brms.client.rpc.BuilderResult;
import org.drools.brms.client.rpc.RepositoryServiceFactory;
import org.drools.brms.client.rpc.RuleAsset;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This widget wraps a rule asset widget, and provides actions to validate and view source.
 * @author Michael Neale
 */
public class RuleValidatorWrapper extends DirtyableComposite {

    private RuleAsset asset;
    private DirtyableFlexTable layout;

    public boolean isDirty() {
        return layout.hasDirty();
    }

    public RuleValidatorWrapper(Widget editor, RuleAsset asset) {
        this.asset = asset;

        layout = new DirtyableFlexTable();
        layout.setStyleName( "asset-editor-Layout" );
        layout.setWidget( 0, 0, editor );
        if (!asset.isreadonly)
            layout.setWidget( 1, 0, validatorActions() );
        layout.getCellFormatter().setAlignment( 1, 0, HasHorizontalAlignment.ALIGN_RIGHT, HasVerticalAlignment.ALIGN_TOP );


        layout.setWidth("100%");
        layout.setHeight( "100%" );

        initWidget( layout );
    }

    private Widget validatorActions() {
        HorizontalPanel horiz = new HorizontalPanel();
        Button viewSource = new Button("View source");
        horiz.add( viewSource );

        Button validate = new Button("Validate");
        horiz.add( validate );

        viewSource.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                doViewsource();
            }

        });

        validate.addClickListener( new ClickListener() {
            public void onClick(Widget w) {
                doValidate();
            }
        });

        horiz.setStyleName( "asset-validator-Buttons" );
        return horiz;
    }

    private void doValidate() {

        LoadingPopup.showMessage( "Validating item, please wait..." );
        RepositoryServiceFactory.getService().buildAsset( asset, new GenericCallback() {
            public void onSuccess(Object data) {
                BuilderResult[] results = (BuilderResult[]) data;
                showBuilderErrors(results);
            }
        });

    }

    private void doViewsource() {
        LoadingPopup.showMessage( "Calculating source..." );
        RepositoryServiceFactory.getService().buildAssetSource( this.asset, new GenericCallback() {
            public void onSuccess(Object data) {
                String src = (String) data;
                showSource(src);
            }
        });

    }

    private void showSource(String src) {
        PackageBuilderWidget.showSource( src, this.asset.metaData.name );
        LoadingPopup.close();
    }

    private void showBuilderErrors(BuilderResult[] results) {
        FormStylePopup pop = new FormStylePopup("images/package_builder.png", "Validation results");
        if (results == null || results.length == 0) {
            pop.addRow( new HTML("<img src='images/tick_green.gif'/><i>Rule built successfully.</i>") );
        } else {
            FlexTable errTable = new FlexTable();
            errTable.setStyleName( "build-Results" );
            for ( int i = 0; i < results.length; i++ ) {
                int row = i;
                final BuilderResult res = results[i];
                errTable.setWidget( row, 0, new Image("images/error.gif"));
                if( res.assetFormat.equals( "package" )) {
                    errTable.setText( row, 1, "[package configuration problem] " + res.message );
                } else {
                    errTable.setText( row, 1, res.message );
                }

            }
            ScrollPanel scroll = new ScrollPanel(errTable);
            //scroll.setAlwaysShowScrollBars(true);
            //scroll.setSize("100%","25em");
            scroll.setWidth( "100%" );
            //scroll.setScrollPosition( 100 );
            //errTable.setWidth( "60%" );
            pop.addRow( scroll );
//            pop.setWidth( "70%" );
//            pop.setHeight( "50%" );

        }
        pop.setPopupPosition( 100, 100 );
        pop.show();
        LoadingPopup.close();
    }



}