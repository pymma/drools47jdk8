package org.drools.brms.server.contenthandler;
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



import java.io.IOException;
import java.io.StringReader;
import java.util.StringTokenizer;

import org.drools.brms.server.builder.BRMSPackageBuilder;
import org.drools.brms.server.builder.ContentPackageAssembler;
import org.drools.compiler.DroolsParserException;
import org.drools.repository.AssetItem;

public class DRLFileContentHandler extends PlainTextContentHandler implements IRuleAsset {

    public void compile(BRMSPackageBuilder builder, AssetItem asset, ContentPackageAssembler.ErrorLogger logger) throws DroolsParserException, IOException {
        builder.addPackageFromDrl( new StringReader(getContent( asset )) );
    }

    private String getContent(AssetItem asset) {
        String content = asset.getContent();
        if (isStandAloneRule( content )) {
            content = wrapRuleDeclaration( asset, content );
        }
        return content;
    }

    private String wrapRuleDeclaration(AssetItem asset, String content) {
        return "rule '" + asset.getName() + "'\n"  + content + "\nend";
    }

    /**
     * This will try and sniff ouf if its a stand alone rule which
     * will use the asset name as the rule name, or if it should be treated as a package
     * (in the latter case, the content is passed as it to the compiler).
     */
    static boolean isStandAloneRule(String content) {
        if (content == null || "".equals( content.trim() )) {
            return false;
        }
        StringTokenizer st = new StringTokenizer(content, " ");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals( "package" ) || 
                    tok.equals( "rule" ) || 
                    tok.equals( "end" ) ||
                    tok.equals( "function" )) {
                return false;
            }
        }
        return true;
        
    }

    public void assembleDRL(BRMSPackageBuilder builder, AssetItem asset, StringBuffer buf) {
        String content = asset.getContent();
        boolean standAlone = isStandAloneRule( content );
        if (standAlone) {
            buf.append( wrapRuleDeclaration( asset, content ) );
        } else {
            buf.append( content );
        }
    }
}