package org.drools.decisiontable.parser;

import java.util.Map;

import org.drools.StatefulSession;

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

/**
 * @author <a href="mailto:stevearoonie@gmail.com">Steven Williams</a>
 * 
 * A cell in a decision table containing a long value
 */
public class LongCell implements Cell {
	Row row;

	Long value;

	Column column;

	private int index;

	public LongCell() {
		
	}
	LongCell(Row r, Column c) {
		row = r;
		column = c;
	}

	public String toString() {
		return "Cell[" + column + ": " + value + "]";
	}

	public Row getRow() {
		return row;
	}

	public Column getColumn() {
		return column;
	}

	public Long getValue() {
		return value;
	}

	public void addValue(Map vars) {
		vars.put(column.getName(), value);
	}
	public void insert(StatefulSession session) {
		session.insert(this);
	}
	public void setIndex(int i) {
		index = i;
	}
	public int getIndex() {
		return index;
	}
	public void setValue(String v) {
		value = new Long(v);
	}
	public boolean isEmpty() {
		return value == null;
	}
}
