package org.drools.decisiontable.parser;

import org.drools.util.StringUtils;

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
 * Base column of in a decision table
 */
public abstract class AbstractColumn implements Column {
	private String name;

	public AbstractColumn(String n) {
		this.name = n;
	}

	public String getName() {
		return name;
	}

	public String getCondition(String condition, int index) {
		StringBuffer conditionString = new StringBuffer(getCellType());
		conditionString.append("(row == r, column == $param");
		if (index != -1) {
			conditionString.append(", index == ").append(index);
		}
		if (!StringUtils.isEmpty(condition)) {
			conditionString.append(", value ").append(condition);
		}
		conditionString.append(")");
		return conditionString.toString();
	}
	
	
}
