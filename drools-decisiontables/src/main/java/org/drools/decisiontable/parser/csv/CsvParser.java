package org.drools.decisiontable.parser.csv;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.drools.decisiontable.parser.DecisionTableParseException;
import org.drools.decisiontable.parser.DecisionTableParser;
import org.drools.decisiontable.parser.SheetListener;

/**
 * Csv implementation. This implementation removes empty "cells" at the end of
 * each line. Different CSV tools may or may not put heaps of empty cells in.
 * 
 * Csv format is almost identical to XLS, with the one limitation: Merged cells
 * are not supported. To allow constraints to span across cells for the one
 * column, this is achieved by using "..." at the end of a cell value. If a cell
 * value ends with "..." then it will be taken as spanned from the previous
 * cell.
 * 
 * 
 * 
 * @author <a href="mailto:michael.neale@gmail.com"> Michael Neale</a>
 */
public class CsvParser implements DecisionTableParser {

	private List _listeners;

	private CsvLineParser _lineParser;

	public CsvParser(final SheetListener listener,
			final CsvLineParser lineParser) {
		_listeners = new ArrayList();
		_listeners.add(listener);
		this._lineParser = lineParser;
	}

	public CsvParser(final List listeners, final CsvLineParser lineParser) {
		this._listeners = listeners;
		this._lineParser = lineParser;
	}

	public void parseFile(final InputStream inStream) {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				inStream));
		try {
			startSheet();
			processRows(reader);
			finishSheet();
		} catch (final IOException e) {
			throw new DecisionTableParseException(
					"An error occurred reading the CSV data.", e);
		}
	}

	private void startSheet() {
		for (Iterator it = _listeners.iterator(); it.hasNext();) {
			SheetListener listener = (SheetListener) it.next();
			listener.startSheet("csv");
		}
	}

	private void finishSheet() {
		for (Iterator it = _listeners.iterator(); it.hasNext();) {
			SheetListener listener = (SheetListener) it.next();
			listener.finishSheet();
		}
	}

	private void newRow(final int row, final int numCells) {
		for (Iterator it = _listeners.iterator(); it.hasNext();) {
			SheetListener listener = (SheetListener) it.next();
			listener.newRow(row, numCells);
		}
	}

	private void newCell(final int row, final int column, final String value,
			final int mergedColStart) {
		for (Iterator it = _listeners.iterator(); it.hasNext();) {
			SheetListener listener = (SheetListener) it.next();
			listener.newCell(row, column, value, mergedColStart);
		}
	}

	private void processRows(final BufferedReader reader) throws IOException {
		String line = reader.readLine();

		int row = 0;
		while (line != null) {

			final List cells = this._lineParser.parse(line);
			// remove the trailing empty "cells" which some tools smatter around
			// trimCells(cells);
			newRow(row, cells.size());

			int startMergeCol = SheetListener.NON_MERGED;
			for (int col = 0; col < cells.size(); col++) {
				String cell = (String) cells.get(col);

				startMergeCol = calcStartMerge(startMergeCol, col, cell);

				cell = calcCellText(startMergeCol, cell);

				newCell(row, col, cell, startMergeCol);
			}
			row++;
			line = reader.readLine();
		}
		finishSheet();
	}

	String calcCellText(int startMergeCol, String cell) {
		if (startMergeCol != SheetListener.NON_MERGED) {
			cell = cell.substring(0, cell.length() - 3);
		}
		return cell;
	}

	int calcStartMerge(int startMergeCol, int col, String cell) {
		if (cell.endsWith("...") && startMergeCol == SheetListener.NON_MERGED) {
			startMergeCol = col;
		} else if (!cell.endsWith("...")) {
			startMergeCol = SheetListener.NON_MERGED;
		}
		return startMergeCol;
	}

	/** remove the trailing empty cells */
	private void trimCells(final List cells) {
		for (int i = cells.size() - 1; i > 0; i--) {
			final String cell = (String) cells.get(i);
			if (!cell.trim().equals("")) {
				return;
			} else {
				cells.remove(i);
			}
		}

	}

}