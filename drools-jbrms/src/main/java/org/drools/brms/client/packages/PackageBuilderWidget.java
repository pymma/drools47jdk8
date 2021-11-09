package org.drools.brms.client.packages;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.drools.brms.client.common.DirtyableComposite;
import org.drools.brms.client.common.ErrorPopup;
import org.drools.brms.client.common.FormStyleLayout;
import org.drools.brms.client.common.FormStylePopup;
import org.drools.brms.client.common.GenericCallback;
import org.drools.brms.client.common.InfoPopup;
import org.drools.brms.client.common.LoadingPopup;
import org.drools.brms.client.rpc.BuilderResult;
import org.drools.brms.client.rpc.PackageConfigData;
import org.drools.brms.client.rpc.RepositoryServiceFactory;
import org.drools.brms.client.rpc.SnapshotInfo;
import org.drools.brms.client.rulelist.EditItemEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This is the widget for building packages, validating etc. Visually decorates
 * or wraps a rule editor widget with buttons for this purpose.
 *
 * @author Michael Neale
 */
public class PackageBuilderWidget extends Composite {

	public FormStyleLayout layout;
	private PackageConfigData conf;
	private EditItemEvent editEvent;

	public PackageBuilderWidget(final PackageConfigData conf,
			EditItemEvent editEvent) {
		layout = new FormStyleLayout("images/package_builder.png",
				"Verify and assemble package");
		this.conf = conf;
		this.editEvent = editEvent;

		final SimplePanel buildResults = new SimplePanel();


        final TextBox selector = new TextBox();
		final Button b = new Button("Build package");
		b.setTitle("This will validate and compile all the assets in a package.");
		b.addClickListener(new ClickListener() {
			public void onClick(Widget w) {
				doBuild(buildResults, selector.getText());
			}
		});

		Button buildSource = new Button("Show package source");
		buildSource.addClickListener(new ClickListener() {
			public void onClick(Widget w) {
				doBuildSource(conf.uuid, conf.name);
			}
		});
		layout.addAttribute("View source for package", buildSource);

        HorizontalPanel buildStuff = new HorizontalPanel();
        buildStuff.add( b );
        buildStuff.add( new HTML("&nbsp;&nbsp;<i>(Optional) selector name: </i>") );
        buildStuff.add( selector );
        buildStuff.add( new InfoPopup("Custom selector", "A selector is configured by administrators to choose what assets form part of a package build. " +
                "This is configured on the server side. The name given is the name of the configuration that the administrator has set." +
                " This is an optional feature (if you don't know what it is, you probably don't need to use it).") );

		layout.addAttribute("Build binary package:", buildStuff);
		layout
				.addRow(new HTML(
						"<i><small>Building a package will collect all the assets, validate and compile into a deployable package.</small></i>"));
		layout.addRow(buildResults);

		layout.setStyleName("package-Editor");

		layout.setWidth("100%");

		initWidget(layout);
	}

	/**
	 * Actually build the source, and display it.
	 */
	public static void doBuildSource(final String uuid, final String name) {
		LoadingPopup.showMessage("Assembling package source...");
		DeferredCommand.add(new Command() {
			public void execute() {
				RepositoryServiceFactory.getService().buildPackageSource(uuid,
						new GenericCallback() {
							public void onSuccess(Object data) {
								String content = (String) data;
								showSource(content, name);
							}
						});
			}
		});
	}

	/**
	 * Popup the view source dialog, showing the given content.
	 */
	public static void showSource(final String content, String name) {
		FormStylePopup pop = new FormStylePopup("images/view_source.gif",
				"Viewing source for: " + name);
		final TextArea area = new TextArea();
		area.setVisibleLines(30);
		area.setWidth("100%");
		area.setCharacterWidth(80);
		pop.addRow(area);
		area.setText(content);
		area.setEnabled(true);
		area
				.setTitle("THIS IS READ ONLY - you may copy and paste, but not edit.");

		area.addKeyboardListener(new KeyboardListener() {

			public void onKeyDown(Widget arg0, char arg1, int arg2) {
				area.setText(content);
			}

			public void onKeyPress(Widget arg0, char arg1, int arg2) {
				area.setText(content);
			}

			public void onKeyUp(Widget arg0, char arg1, int arg2) {
				area.setText(content);
			}

		});

		LoadingPopup.close();
		pop.setPopupPosition((DirtyableComposite.getWidth() - pop
				.getOffsetWidth()) / 2, 100);
		pop.show();

	}

	/**
	 * Actually do the building.
	 *
	 * @param buildResults
	 *            The panel to stuff the results in.
	 * @param selectorName
	 */
	private void doBuild(final Panel buildResults, final String selectorName) {

		buildResults.clear();

		final HorizontalPanel busy = new HorizontalPanel();
		busy.add(new Label("Validating and building package, please wait..."));
		busy.add(new Image("images/red_anime.gif"));

        LoadingPopup.showMessage( "Please wait..." );
		buildResults.add(busy);

		DeferredCommand.add(new Command() {
			public void execute() {
				RepositoryServiceFactory.getService().buildPackage(conf.uuid, selectorName,
						new GenericCallback() {
							public void onSuccess(Object data) {
                                LoadingPopup.close();
								if (data == null) {
									showSuccessfulBuild(buildResults);
								} else {
									BuilderResult[] results = (BuilderResult[]) data;
									showBuilderErrors(results, buildResults);
								}
							}
						});
			}
		});

	}

	/**
	 * This is called to display the success (and a download option).
	 *
	 * @param buildResults
	 */
	private void showSuccessfulBuild(Panel buildResults) {
		buildResults.clear();
		VerticalPanel vert = new VerticalPanel();

		vert.add(new HTML(
						"<img src='images/tick_green.gif'/><i>Package built successfully.</i>"));
		final String hyp = getDownloadLink(this.conf);

		HTML html = new HTML("<a href='" + hyp
				+ "' target='_blank'>Download binary package</a>");

		// Button download = new Button("Download binary package");
		// download.setTitle( "You can download the package here for deployment,
		// or you can use the snapshot deployment feature to have a more
		// permanent downloadable package." );
		// download.addClickListener( new ClickListener() {
		// public void onClick(Widget arg0) {
		// Window.open( hyp, "downloading...",
		// "resizable=no,scrollbars=yes,status=no" );
		// }
		// });

		// vert.add( download );
		vert.add(html);
		Button snap = new Button("Create snapshot for deployment");
		snap.addClickListener(new ClickListener() {
			public void onClick(Widget w) {
				showSnapshotDialog(w);
			}
		});
		vert.add(snap);

		buildResults.add(vert);
	}

	/**
	 * Get a download link for the binary package.
	 */
	public static String getDownloadLink(PackageConfigData conf) {
		String hurl = GWT.getModuleBaseURL() + "package/" + conf.name;
		if (!conf.isSnapshot) {
			hurl = hurl + "/" + PackageSnapshotView.LATEST_SNAPSHOT;
		} else {
			hurl = hurl + "/" + conf.snapshotName;
		}
		final String uri = hurl;
		return uri;
	}

	/**
	 * This is called in the unhappy event of there being errors.
	 */
	private void showBuilderErrors(BuilderResult[] results, Panel buildResults) {
		buildResults.clear();

		FlexTable errTable = new FlexTable();
		errTable.setStyleName("build-Results");
		errTable.setText(0, 1, "Format");
		errTable.setText(0, 2, "Name");
		errTable.setText(0, 3, "Message");

		for (int i = 0; i < results.length; i++) {
			int row = i + 1;
			final BuilderResult res = results[i];
			errTable.setWidget(row, 0, new Image("images/error.gif"));
			errTable.setText(row, 1, res.assetFormat);
			errTable.setText(row, 2, res.assetName);
			errTable.setText(row, 3, res.message);

			if (!"package".equals(res.assetFormat)) {
				Button show = new Button("Show");
				show.addClickListener(new ClickListener() {
					public void onClick(Widget w) {
						editEvent.open(res.uuid);
					}
				});
				errTable.setWidget(row, 4, show);
			}
		}

		errTable.setWidth("100%");
		ScrollPanel scroll = new ScrollPanel(errTable);
		scroll.setAlwaysShowScrollBars(true);
		scroll.setSize("100%", "25em");

		buildResults.add(scroll);

	}

	/**
	 * This will display a dialog for creating a snapshot.
	 */
	private void showSnapshotDialog(Widget w) {
		LoadingPopup.showMessage("Loading existing snapshots...");
		final FormStylePopup form = new FormStylePopup("images/snapshot.png",
				"Create a snapshot for deployment.");
		form
				.addRow(new HTML(
						"<i>A package snapshot is essentially a "
								+ "read only 'locked in' and labelled view of a package at a point in time, which can be used for deployment.</i>"));

		final VerticalPanel vert = new VerticalPanel();
		form.addAttribute("Choose or create snapshot name:", vert);
		final List radioList = new ArrayList();
		final TextBox newName = new TextBox();
		final String newSnapshotText = "NEW: ";

		RepositoryServiceFactory.getService().listSnapshots(conf.name,
				new GenericCallback() {
					public void onSuccess(Object data) {
						SnapshotInfo[] result = (SnapshotInfo[]) data;
						for (int i = 0; i < result.length; i++) {
							RadioButton existing = new RadioButton(
									"snapshotNameGroup", result[i].name);
							radioList.add(existing);
							vert.add(existing);
						}
						HorizontalPanel newSnap = new HorizontalPanel();

						final RadioButton newSnapRadio = new RadioButton(
								"snapshotNameGroup", newSnapshotText);
						newSnap.add(newSnapRadio);
						newName.setEnabled(false);
						newSnapRadio.addClickListener(new ClickListener() {

							public void onClick(Widget w) {
								newName.setEnabled(true);
							}

						});

						newSnap.add(newName);
						radioList.add(newSnapRadio);
						vert.add(newSnap);

						LoadingPopup.close();
					}
				});

		final TextBox comment = new TextBox();
		form.addAttribute("Comment:", comment);

		Button create = new Button("Create new snapshot");
		form.addAttribute("", create);

		create.addClickListener(new ClickListener() {
			String name = "";

			public void onClick(Widget w) {
				boolean replace = false;
				for (Iterator iter = radioList.iterator(); iter.hasNext();) {
					RadioButton but = (RadioButton) iter.next();
					if (but.isChecked()) {
						name = but.getText();
						if (!but.getText().equals(newSnapshotText)) {
							replace = true;
						}
						break;
					}
				}
				if (name.equals(newSnapshotText)) {
					name = newName.getText();
				}

				if (name.equals("")) {
					Window
							.alert("You have to enter or chose a label (name) for the snapshot.");
					return;
				}

				RepositoryServiceFactory.getService().createPackageSnapshot(
						conf.name, name, replace, comment.getText(),
						new GenericCallback() {
							public void onSuccess(Object data) {
								Window.alert("The snapshot called: " + name
										+ " was successfully created.");
								form.hide();
							}
						});
			}
		});

		form.setWidth("50%");

		form.setPopupPosition((DirtyableComposite.getWidth() - form
				.getOffsetWidth()) / 2, 100);
		form.show();

		// form.setPopupPosition( Window.getClientWidth() / 3,
		// Window.getClientHeight() / 3 );
		// form.show();

	}

}